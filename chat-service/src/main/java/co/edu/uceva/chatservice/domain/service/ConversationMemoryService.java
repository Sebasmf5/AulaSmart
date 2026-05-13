package co.edu.uceva.chatservice.domain.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de memoria de conversación en memoria.
 * Almacena los últimos N mensajes (user + assistant) por identificador de usuario.
 * Esto permite que el chatbot mantenga contexto entre mensajes consecutivos.
 */
@Service
public class ConversationMemoryService {

    private static final int MAX_MESSAGES = 20; // 10 pares de conversación

    private final Map<String, List<Message>> memory = new ConcurrentHashMap<>();

    public void addUserMessage(String userId, String content) {
        memory.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new UserMessage(content));
        trim(userId);
    }

    public void addAssistantMessage(String userId, String content) {
        memory.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new AssistantMessage(content));
        trim(userId);
    }

    public List<Message> getHistory(String userId) {
        return new ArrayList<>(memory.getOrDefault(userId, Collections.emptyList()));
    }

    public void clear(String userId) {
        memory.remove(userId);
    }

    private void trim(String userId) {
        List<Message> list = memory.get(userId);
        if (list != null && list.size() > MAX_MESSAGES) {
            // Eliminar los mensajes más antiguos
            list.subList(0, list.size() - MAX_MESSAGES).clear();
        }
    }
}
