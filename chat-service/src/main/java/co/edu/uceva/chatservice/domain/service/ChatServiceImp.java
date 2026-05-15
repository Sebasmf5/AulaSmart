package co.edu.uceva.chatservice.domain.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ChatServiceImp implements ChatService{

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
        String mensajeConFecha = "[Hoy es " + fechaHoy + "] " + mensaje;

        System.out.println("[ChatServiceImp] User: " + userId + " | Mensaje: " + mensajeConFecha);

        // 1. Verificar caché (solo para mensajes de consulta simple, no reservas)
        if (esConsultaCacheable(mensaje)) {
            String cacheKey = construirCacheKey(userId, mensaje, fechaHoy);
            String cached = cacheService.get(cacheKey);
            if (cached != null) {
                System.out.println("[ChatServiceImp] RESPUESTA DESDE CACHÉ");
                return cached;
            }
        }

        // 2. Guardar mensaje del usuario en memoria
        memoryService.addUserMessage(userId, mensajeConFecha);

        // 3. Recuperar historial de la conversación
        List<org.springframework.ai.chat.messages.Message> historial = memoryService.getHistory(userId);

        // 4. Llamar al LLM con historial completo
        String respuesta;
        try {
            respuesta = chatClient.prompt()
                    .messages(historial)
                    .call()
                    .content();
        } catch (Exception e) {
            System.err.println("[ChatServiceImp] Error llamando al LLM: " + e.getMessage());
            respuesta = "Lo siento, estoy teniendo problemas para conectar con el sistema de inteligencia artificial. " +
                    "Por favor intenta de nuevo en unos momentos. Si el problema persiste, contacta al administrador.";
        }

        // 5. Guardar respuesta del asistente en memoria
        memoryService.addAssistantMessage(userId, respuesta);

        // 6. Guardar en caché si es consulta simple
        if (esConsultaCacheable(mensaje)) {
            String cacheKey = construirCacheKey(userId, mensaje, fechaHoy);
            cacheService.put(cacheKey, respuesta);
        }

        return respuesta;
    }

    /**
     * Determina si un mensaje es susceptible de ser cacheado.
     * NO cacheamos reservas ni operaciones de escritura por obvias razones.
     */
    private boolean esConsultaCacheable(String mensaje) {
        String lower = mensaje.toLowerCase();
        // Solo cacheamos consultas puras de disponibilidad
        boolean esConsulta = lower.contains("disponible") || lower.contains("hay") || 
                            lower.contains("libre") || lower.contains("ocupado") ||
                            lower.contains("aulas") || lower.contains("horario");
        boolean esReserva = lower.contains("reserv") || lower.contains("agendar") || 
                           lower.contains("separar") || lower.contains("confirm");
        return esConsulta && !esReserva;
    }

    /**
     * Construye una clave de caché normalizada.
     * Elimina variaciones de mayúsculas/minúsculas y espacios extras.
     */
    private String construirCacheKey(String userId, String mensaje, String fecha) {
        String normalizado = mensaje.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9áéíóúñ ]", "")
                .trim();
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
