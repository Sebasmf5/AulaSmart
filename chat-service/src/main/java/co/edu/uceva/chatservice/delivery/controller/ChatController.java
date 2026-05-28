package co.edu.uceva.chatservice.delivery.controller;

import co.edu.uceva.chatservice.domain.model.ChatRequest;
import co.edu.uceva.chatservice.domain.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatClient chatClient;

    // Inyectamos nuestro servicio de IA
    public ChatController(ChatService chatService, ChatClient chatClient) {
        this.chatService = chatService;
        this.chatClient = chatClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> hablarConIA(@Valid @RequestBody ChatRequest request) {
        String respuesta = chatService.procesarMensaje(request.mensaje());
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    /**
     * Reinicia la conversación del usuario autenticado.
     * Borra el historial de mensajes y la caché asociada.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reiniciarConversacion() {
        chatService.reiniciarConversacion();
        return ResponseEntity.ok(Map.of("mensaje", "Conversación reiniciada exitosamente."));
    }

    /**
     * Endpoint de diagnóstico para probar la conexión directa con el LLM.
     * No usa memoria ni caché. Útil para depurar problemas con el proveedor de IA.
     */
    @GetMapping("/health/llm")
    public ResponseEntity<Map<String, Object>> diagnosticarLLM() {
        try {
            String respuesta = chatClient.prompt()
                    .user("Responde únicamente con la palabra 'OK' sin ningún otro texto.")
                    .call()
                    .content();
            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "respuesta", respuesta != null ? respuesta : "NULL",
                "proveedor", "OpenCode Go / kimi-k2.6"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "error", e.getClass().getName(),
                "message", e.getMessage(),
                "cause", e.getCause() != null ? e.getCause().getMessage() : "null"
            ));
        }
    }
}
