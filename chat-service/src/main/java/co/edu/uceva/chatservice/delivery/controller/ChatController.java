package co.edu.uceva.chatservice.delivery.controller;

import co.edu.uceva.chatservice.domain.model.ChatRequest;
import co.edu.uceva.chatservice.domain.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    // Inyectamos nuestro servicio de IA
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public String hablarConIA(@RequestBody ChatRequest request) {
        return chatService.procesarMensaje(request.mensaje());
    }
}