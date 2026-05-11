package co.edu.uceva.chatservice.domain.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImp implements ChatService{

    private final ChatClient chatClient;

    public ChatServiceImp(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String procesarMensaje(String mensaje) {
        return chatClient.prompt()
                .user(mensaje)
                .toolNames("consultarPorBloque", "consultarPorTipo", "consultarPorNombreAula", "reservarAulaTool")
                .call()
                .content();
    }
}
