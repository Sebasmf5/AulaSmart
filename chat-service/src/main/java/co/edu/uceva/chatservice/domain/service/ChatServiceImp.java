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

    public ChatServiceImp(ChatClient chatClient, ConversationMemoryService memoryService) {
        this.chatClient = chatClient;
        this.memoryService = memoryService;
    }

    @Override
    public String procesarMensaje(String mensaje) {
        String fechaHoy = LocalDate.now().toString(); // yyyy-MM-dd
        String userId = obtenerUserId();
        String mensajeConFecha = "[Hoy es " + fechaHoy + "] " + mensaje;

        System.out.println("[ChatServiceImp] User: " + userId + " | Mensaje: " + mensajeConFecha);

        // 1. Guardar mensaje del usuario en memoria
        memoryService.addUserMessage(userId, mensajeConFecha);

        // 2. Recuperar historial de la conversación
        List<org.springframework.ai.chat.messages.Message> historial = memoryService.getHistory(userId);

        // 3. Llamar al LLM con historial completo
        String respuesta = chatClient.prompt()
                .messages(historial)
                .call()
                .content();

        // 4. Guardar respuesta del asistente en memoria
        memoryService.addAssistantMessage(userId, respuesta);

        return respuesta;
    }

    private String obtenerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
