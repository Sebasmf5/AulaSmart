package co.edu.uceva.chatservice.domain.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de memoria de conversación con Caffeine.
 * Almacena los últimos N mensajes (user + assistant) por identificador de usuario.
 * TTL de 30 minutos de inactividad, máximo 5.000 usuarios en caché.
 * Esto permite que el chatbot mantenga contexto entre mensajes consecutivos.
 *
 * El historial devuelto al LLM está limitado a los últimos 8 mensajes
 * para controlar el consumo de tokens. Los mensajes más antiguos se mantienen en
 * memoria interna (hasta 12) pero no se envían al LLM.
 */
@Service
@Slf4j
public class ConversationMemoryService {

    private static final int MAX_MESSAGES_GUARDADOS = 12; // 6 pares de conversación en memoria
    private static final int MAX_MESSAGES_LLM = 8;        // Solo 4 pares se envían al LLM

    private final Cache<String, List<Message>> cache;

    public ConversationMemoryService() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener((key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("[Memory] Sesión expirada/eliminada: userId={}, cause={}", key, cause);
                    }
                })
                .build();
    }

    public void addUserMessage(String userId, String content) {
        List<Message> list = cache.get(userId, k -> new CopyOnWriteArrayList<>());
        list.add(new UserMessage(content));
        trim(userId, list);
    }

    public void addAssistantMessage(String userId, String content) {
        List<Message> list = cache.get(userId, k -> new CopyOnWriteArrayList<>());
        list.add(new AssistantMessage(content));
        trim(userId, list);
    }

    /**
     * Devuelve el historial reciente para enviar al LLM.
     * Limitado a MAX_MESSAGES_LLM mensajes para controlar tokens.
     * Esto evita que conversaciones largas consuman miles de tokens innecesariamente.
     */
    public List<Message> getHistory(String userId) {
        List<Message> todos = cache.getIfPresent(userId);
        if (todos == null || todos.size() <= MAX_MESSAGES_LLM) {
            return todos != null ? new ArrayList<>(todos) : new ArrayList<>();
        }
        // Solo devolver los últimos 8 mensajes (4 pares de conversación)
        // Esto es suficiente para follow-ups inmediatos sin arrastrar todo el historial
        return new ArrayList<>(todos.subList(todos.size() - MAX_MESSAGES_LLM, todos.size()));
    }

    public void clear(String userId) {
        cache.invalidate(userId);
        log.info("[Memory] Conversación reiniciada para userId={}", userId);
    }

    private void trim(String userId, List<Message> list) {
        if (list == null) return;
        // CopyOnWriteArrayList no requiere synchronized explícito para lecturas,
        // pero la operación compuesta size() + subList().clear() sí necesita
        // sincronización para evitar race conditions entre hilos concurrentes.
        synchronized (list) {
            if (list.size() > MAX_MESSAGES_GUARDADOS) {
                list.subList(0, list.size() - MAX_MESSAGES_GUARDADOS).clear();
            }
        }
    }
}
