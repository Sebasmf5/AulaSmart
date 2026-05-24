package co.edu.uceva.chatservice.delivery.controller;

import co.edu.uceva.chatservice.domain.model.ChatRequest;
import co.edu.uceva.chatservice.domain.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    // Inyectamos nuestro servicio de IA
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
}
