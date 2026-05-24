package co.edu.uceva.chatservice.domain.service;

import co.edu.uceva.chatservice.domain.config.AuthContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
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

        // 3. Recuperar historial de la conversación (limitado a últimos 8 mensajes)
        List<org.springframework.ai.chat.messages.Message> historial = memoryService.getHistory(userId);

        // 4. Agregar system message con fecha al inicio
        List<org.springframework.ai.chat.messages.Message> mensajesParaLLM = new ArrayList<>();
        mensajesParaLLM.add(new SystemMessage("Hoy es " + fechaHoy + ". Usa esta fecha para interpretar relativos como 'hoy', 'mañana', 'pasado mañana'."));
        mensajesParaLLM.addAll(historial);

        // 5. Llamar al LLM con historial + fecha (con timeout de 30 segundos)
        //    Capturar JWT del hilo actual y propagarlo al hilo asíncrono donde Spring AI ejecuta las tools
        String jwtActual = AuthContext.getJwt();
        log.debug("[ChatServiceImp] JWT capturado para propagación async: {}", jwtActual != null ? "presente" : "ausente");

        String respuesta;
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                // Restaurar JWT en el hilo de ForkJoinPool para que FeignInterceptor lo envíe a los microservicios
                if (jwtActual != null) {
                    AuthContext.setJwt(jwtActual);
                }
                try {
                    return chatClient.prompt()
                            .messages(mensajesParaLLM)
                            .call()
                            .content();
                } finally {
                    AuthContext.clear();
                }
            });
            respuesta = future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[ChatServiceImp] Timeout llamando al LLM después de 30s para user={}", userId);
            respuesta = "La consulta tardó demasiado. Por favor intenta de nuevo.";
        } catch (Exception e) {
            log.error("[ChatServiceImp] Error llamando al LLM para user={}", userId, e);
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

    /**
     * Si la respuesta es una lista larga de aulas (tool call), construye un resumen
     * que conserve los primeros ítems útiles para follow-ups, sin arrastrar todo el texto.
     * La respuesta completa se devuelve al usuario; en memoria solo guardamos el resumen.
     */
    private String resumirRespuestaLarga(String respuesta) {
        if (respuesta == null || respuesta.length() < 300) {
            return respuesta; // No resumir si es corta
        }

        // Detectar listas de aulas disponibles
        if (respuesta.contains("Aulas disponibles") || respuesta.contains("Aulas de tipo")) {
            String[] lineas = respuesta.split("\n");
            StringBuilder resumen = new StringBuilder();
            int count = 0;
            for (String linea : lineas) {
                String trim = linea.trim();
                if (trim.isEmpty()) continue;
                // Conservar título y primeros 3 ítems de la lista
                if (count == 0 || (trim.startsWith("-") || trim.startsWith("•")) && count <= 3) {
                    resumen.append(trim).append("\n");
                    count++;
                } else if (count > 3) {
                    break;
                }
            }
            int totalLineas = 0;
            for (String linea : lineas) {
                String trim = linea.trim();
                if (!trim.isEmpty() && (trim.startsWith("-") || trim.startsWith("•"))) {
                    totalLineas++;
                }
            }
            resumen.append("[Resumen: ").append(totalLineas)
                   .append(" aulas mostradas al usuario. Responde sobre aulas específicas si el usuario menciona una.]");
            return resumen.toString();
        }

        // Detectar horarios ocupados/libres
        if (respuesta.contains("Horarios del aula") && respuesta.length() > 500) {
            String[] lineas = respuesta.split("\n");
            StringBuilder resumen = new StringBuilder();
            // Conservar primera línea (título) y la última línea de rango
            if (lineas.length > 0) resumen.append(lineas[0]).append("\n");
            resumen.append("[Resumen: horarios consultados y mostrados al usuario.]");
            return resumen.toString();
        }

        return respuesta;
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
