package co.edu.uceva.chatservice.domain.service;

import co.edu.uceva.chatservice.domain.config.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ChatServiceImp implements ChatService {

    private final ChatClient chatClient;
    private final ConversationMemoryService memoryService;
    private final ChatCacheService cacheService;

    @Value("${chat.system-prompt}")
    private String systemPrompt;

    public ChatServiceImp(ChatClient chatClient, ConversationMemoryService memoryService, ChatCacheService cacheService) {
        this.chatClient = chatClient;
        this.memoryService = memoryService;
        this.cacheService = cacheService;
    }

    @Override
    public String procesarMensaje(String mensaje) {
        String fechaHoy = LocalDate.now().toString(); // yyyy-MM-dd
        String userId = obtenerUserId();

        // Garantizar JWT en ThreadLocal para tool calls en hilos secundarios
        if (AuthContext.getJwt() == null) {
            // Si llegamos aquí sin JWT en ThreadLocal, el JwtPropagationFilter
            // no lo capturó (ej: test, llamada interna). Log de advertencia.
            log.warn("[ChatServiceImp] JWT no disponible en ThreadLocal para user: {}", userId);
        }

        log.info("[ChatServiceImp] User: {} | Mensaje: {}", userId, mensaje);

        // 1. Verificar caché (solo para mensajes de consulta simple, no reservas)
        if (esConsultaCacheable(mensaje)) {
            String cacheKey = construirCacheKey(userId, mensaje, fechaHoy);
            String cached = cacheService.get(cacheKey);
            if (cached != null) {
                log.info("[ChatServiceImp] RESPUESTA DESDE CACHÉ para clave: {}", cacheKey);
                return cached;
            }
        }

        // 2. Guardar mensaje del usuario en memoria (SIN fecha repetida)
        memoryService.addUserMessage(userId, mensaje);

        // 3. Recuperar historial de la conversación (limitado por cantidad + caracteres/tokens)
        List<org.springframework.ai.chat.messages.Message> historial = memoryService.getHistory(userId);
        int estimatedTokens = memoryService.estimateTokens(userId);

        // 4. Construir lista de mensajes para el LLM
        //    CRÍTICO: incluir el system prompt de AulaBot + fecha, ya que .messages() reemplaza
        //    cualquier configuración default del ChatClient builder.
        List<org.springframework.ai.chat.messages.Message> mensajesParaLLM = new ArrayList<>();
        String systemMessageCompleto = systemPrompt + "\n\nHoy es " + fechaHoy +
                ". Usa esta fecha para interpretar relativos como 'hoy', 'mañana', 'pasado mañana'.";
        mensajesParaLLM.add(new SystemMessage(systemMessageCompleto));
        mensajesParaLLM.addAll(historial);

        // 5. Llamar al LLM con historial + system prompt (con timeout de 60 segundos)
        //    Capturar JWT del hilo actual y propagarlo al hilo asíncrono donde Spring AI ejecuta las tools
        String jwtActual = AuthContext.getJwt();
        log.debug("[ChatServiceImp] JWT capturado para propagación async: {}", jwtActual != null ? "presente" : "ausente");

        String respuesta;
        try {
            // LOG completo de lo que se envía al LLM para diagnóstico y control de costos
            log.info("[ChatServiceImp] Enviando {} mensajes al LLM para user={} | ~{} tokens estimados (historial)",
                    mensajesParaLLM.size(), userId, estimatedTokens);
            for (int i = 0; i < mensajesParaLLM.size(); i++) {
                var msg = mensajesParaLLM.get(i);
                int len = msg.getText() != null ? msg.getText().length() : 0;
                log.debug("[ChatServiceImp] MSG[{}] type={} | len={} | content={}", i, msg.getMessageType(), len,
                        msg.getText() != null ? msg.getText().substring(0, Math.min(len, 200)) : "NULL");
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                // Restaurar JWT en el hilo de ForkJoinPool para que FeignInterceptor lo envíe a los microservicios
                if (jwtActual != null) {
                    AuthContext.setJwt(jwtActual);
                }
                try {
                    String result = chatClient.prompt()
                            .messages(mensajesParaLLM)
                            .call()
                            .content();
                    log.info("[ChatServiceImp] LLM raw response (first 200 chars): {}", 
                             result != null ? result.substring(0, Math.min(result.length(), 200)) : "NULL");
                    return result;
                } finally {
                    AuthContext.clear();
                }
            });
            respuesta = future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[ChatServiceImp] Timeout llamando al LLM después de 60s para user={}", userId);
            respuesta = "La consulta tardó demasiado. Por favor intenta de nuevo.";
        } catch (Exception e) {
            log.error("[ChatServiceImp] Error llamando al LLM para user={}. Exception type: {} | Message: {}", 
                      userId, e.getClass().getName(), e.getMessage(), e);
            // Loggear la causa raíz si es ExecutionException
            Throwable cause = e.getCause();
            if (cause != null) {
                log.error("[ChatServiceImp] Causa raíz: {} | Message: {}", 
                          cause.getClass().getName(), cause.getMessage());
                // Si la causa tiene body/response, intentar extraerlo
                if (cause instanceof com.openai.errors.BadRequestException) {
                    log.error("[ChatServiceImp] BadRequestException details: {}", cause.toString());
                }
            }
            respuesta = "Lo siento, estoy teniendo problemas técnicos. Intenta en unos momentos.";
        }

        // 6. Resumir respuesta larga antes de guardar en memoria (ahorra tokens)
        String respuestaParaMemoria = resumirRespuestaLarga(respuesta);

        // 7. Guardar respuesta del asistente en memoria
        memoryService.addAssistantMessage(userId, respuestaParaMemoria);

        // 8. Guardar en caché si es consulta simple
        if (esConsultaCacheable(mensaje)) {
            String cacheKey = construirCacheKey(userId, mensaje, fechaHoy);
            // TTL diferenciado: consultas de disponibilidad expiran rápido
            Duration ttl = esConsultaDisponibilidad(mensaje)
                    ? ChatCacheService.CACHE_TTL_DISPONIBILIDAD
                    : ChatCacheService.CACHE_TTL_ESTATICO;
            cacheService.put(cacheKey, respuesta, ttl);
        }

        return respuesta;
    }

    @Override
    public void reiniciarConversacion() {
        String userId = obtenerUserId();
        memoryService.clear(userId);
        // Invalidar caché de disponibilidad para evitar datos obsoletos
        cacheService.invalidate("disponible");
        cacheService.invalidate("libre");
        cacheService.invalidate("ocupado");
        cacheService.invalidate("aulas");
        log.info("[ChatServiceImp] Conversación reiniciada y caché de disponibilidad invalidada para user: {}", userId);
    }

    /**
     * Detecta si el mensaje es una consulta de disponibilidad real (cambia rápido).
     */
    private boolean esConsultaDisponibilidad(String mensaje) {
        String lower = mensaje.toLowerCase();
        return lower.contains("disponible")
                || lower.contains("libre")
                || lower.contains("ocupado")
                || lower.contains("hay");
    }

    private static final int MAX_CHARS_MEMORIA = 1_200; // ~300 tokens por respuesta en memoria
    private static final int MAX_CHARS_TRUNCAMIENTO_GENERAL = 2_000;

    /**
     * Resume respuestas largas antes de guardarlas en memoria.
     * Esto evita que una sola respuesta masiva (lista de aulas, horarios, error largo)
     * consuma todo el presupuesto de tokens del historial.
     *
     * Estrategia:
     * 1. Si es corta (<300 chars), no resumir.
     * 2. Si coincide con patrones conocidos (aulas, horarios), resumen semántico.
     * 3. Para cualquier otro texto largo (>MAX_CHARS_TRUNCAMIENTO_GENERAL), truncar
     *    agresivamente + indicador de truncamiento.
     */
    private String resumirRespuestaLarga(String respuesta) {
        if (respuesta == null || respuesta.length() < 300) {
            return respuesta;
        }

        // Patrón 1: listas de aulas disponibles
        if (respuesta.contains("Aulas disponibles") || respuesta.contains("Aulas de tipo")) {
            return resumirListaAulas(respuesta);
        }

        // Patrón 2: horarios ocupados/libres
        if (respuesta.contains("Horarios del aula") && respuesta.length() > 500) {
            return resumirHorarios(respuesta);
        }

        // Fallback general: truncamiento agresivo para cualquier respuesta muy larga
        if (respuesta.length() > MAX_CHARS_TRUNCAMIENTO_GENERAL) {
            String truncada = respuesta.substring(0, MAX_CHARS_MEMORIA);
            // Cortar en el último punto o salto de línea para no dejar palabra cortada
            int lastBreak = Math.max(truncada.lastIndexOf('.'), truncada.lastIndexOf('\n'));
            if (lastBreak > MAX_CHARS_MEMORIA * 0.7) {
                truncada = truncada.substring(0, lastBreak + 1);
            }
            return truncada + "\n[... y " + (respuesta.length() - truncada.length()) +
                    " caracteres más. Resumen: respuesta larga mostrada al usuario.]";
        }

        return respuesta;
    }

    private String resumirListaAulas(String respuesta) {
        String[] lineas = respuesta.split("\n");
        StringBuilder resumen = new StringBuilder();
        int count = 0;
        for (String linea : lineas) {
            String trim = linea.trim();
            if (trim.isEmpty()) continue;
            if (count == 0 || ((trim.startsWith("-") || trim.startsWith("•")) && count <= 3)) {
                resumen.append(trim).append("\n");
                count++;
            } else if (count > 3) {
                break;
            }
        }
        int totalItems = 0;
        for (String linea : lineas) {
            String trim = linea.trim();
            if (!trim.isEmpty() && (trim.startsWith("-") || trim.startsWith("•"))) {
                totalItems++;
            }
        }
        resumen.append("[Resumen: ").append(totalItems)
                .append(" aulas mostradas al usuario. Responde sobre aulas específicas si el usuario menciona una.]");
        return resumen.toString();
    }

    private String resumirHorarios(String respuesta) {
        String[] lineas = respuesta.split("\n");
        StringBuilder resumen = new StringBuilder();
        if (lineas.length > 0) resumen.append(lineas[0]).append("\n");
        resumen.append("[Resumen: horarios consultados y mostrados al usuario.]");
        return resumen.toString();
    }

    /**
     * Determina si un mensaje es susceptible de ser cacheado.
     * NO cacheamos reservas ni operaciones de escritura por obvias razones.
     */
    private boolean esConsultaCacheable(String mensaje) {
        String lower = mensaje.toLowerCase();
        // Detectar consultas generales
        boolean esConsulta = lower.contains("disponible") || lower.contains("hay") ||
                            lower.contains("libre") || lower.contains("ocupado") ||
                            lower.contains("aulas") || lower.contains("horario") ||
                            lower.contains("cuáles") || lower.contains("cuando") ||
                            lower.contains("qué") || lower.contains("dime") ||
                            lower.contains("cuántas") || lower.contains("capacidad");
        // Detectar follow-ups cortos (1-3 palabras, sin verbos de acción)
        boolean esFollowUpCorto = mensaje.trim().split("\\s+").length <= 3 &&
                                 !lower.contains("reserv") && !lower.contains("agendar") &&
                                 !lower.contains("quiero") && !lower.contains("necesito");
        // Excluir reservas explícitas
        boolean esReserva = lower.contains("reserv") || lower.contains("agendar") ||
                           lower.contains("separar") || lower.contains("confirm") ||
                           lower.contains("quiero") || lower.contains("necesito");
        return (esConsulta || esFollowUpCorto) && !esReserva;
    }

    /**
     * Construye una clave de caché normalizada.
     * Para consultas globales (aulas disponibles, horarios generales) se usa una clave
     * global sin userId, permitiendo reutilización entre usuarios.
     * Para consultas personales se usa userId.
     */
    private String construirCacheKey(String userId, String mensaje, String fecha) {
        String normalizado = mensaje.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9áéíóúñ ]", "")
                .trim();
        boolean esGlobal = normalizado.contains("aulas")
                || normalizado.contains("disponible")
                || normalizado.contains("horario")
                || normalizado.contains("libre");
        if (esGlobal) {
            return "global:" + fecha + ":" + normalizado;
        }
        return userId + ":" + fecha + ":" + normalizado;
    }

    private String obtenerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
