package co.edu.uceva.chatservice.delivery.controller;

import co.edu.uceva.chatservice.domain.model.ChatRequest;
import co.edu.uceva.chatservice.domain.service.ChatService;
import co.edu.uceva.chatservice.domain.FeignClients.IAulaServiceClient;
import co.edu.uceva.chatservice.domain.FeignClients.IReservaServiceClient;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatClient chatClient;
    private final IAulaServiceClient aulaClient;
    private final IReservaServiceClient reservaClient;

    public ChatController(ChatService chatService, ChatClient chatClient,
                          IAulaServiceClient aulaClient, IReservaServiceClient reservaClient) {
        this.chatService = chatService;
        this.chatClient = chatClient;
        this.aulaClient = aulaClient;
        this.reservaClient = reservaClient;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> hablarConIA(@Valid @RequestBody ChatRequest request) {
        String respuesta = chatService.procesarMensaje(request.mensaje());
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reiniciarConversacion() {
        chatService.reiniciarConversacion();
        return ResponseEntity.ok(Map.of("mensaje", "Conversación reiniciada exitosamente."));
    }

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

    @GetMapping("/health/services")
    public ResponseEntity<Map<String, Object>> diagnosticarServicios() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chat-service", "OK");

        try {
            long start = System.currentTimeMillis();
            aulaClient.listarBloques();
            long elapsed = System.currentTimeMillis() - start;
            result.put("aula-service", Map.of("status", "OK", "latencia_ms", elapsed));
        } catch (Exception e) {
            result.put("aula-service", Map.of(
                "status", "ERROR",
                "error", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage().substring(0, Math.min(200, e.getMessage().length())) : "null"
            ));
        }

        try {
            long start = System.currentTimeMillis();
            reservaClient.obtenerAulasOcupadas(
                java.time.LocalDate.now().toString(), "08:00", "09:00");
            long elapsed = System.currentTimeMillis() - start;
            result.put("reserva-service", Map.of("status", "OK", "latencia_ms", elapsed));
        } catch (Exception e) {
            result.put("reserva-service", Map.of(
                "status", "ERROR",
                "error", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage().substring(0, Math.min(200, e.getMessage().length())) : "null"
            ));
        }

        return ResponseEntity.ok(result);
    }
}
