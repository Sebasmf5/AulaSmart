package co.edu.uceva.chatservice.domain.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de memoria de conversación con Caffeine.
 * Almacena los últimos N mensajes (user + assistant) por identificador de usuario.
 * TTL de 30 minutos de inactividad, máximo 5.000 usuarios en caché.
 *
 * Estrategia de limitación de contexto (híbrida):
 * - Por cantidad: máximo 12 mensajes en memoria, 8 enviados al LLM.
 * - Por caracteres: máximo ~8.000 chars en memoria (~2.000 tokens), ~4.000 chars al LLM (~1.000 tokens).
 *   Esto evita que respuestas largas de tool calls (listas de aulas, horarios) saturen el contexto.
 *
 * Aproximación: ~4 caracteres = 1 token (válido para español/inglés con modelos modernos).
 */
@Service
@Slf4j
public class ConversationMemoryService {

    // Límites por cantidad de mensajes
    private static final int MAX_MESSAGES_GUARDADOS = 12; // 6 pares
    private static final int MAX_MESSAGES_LLM = 8;        // 4 pares

    // Límites por volumen (aproximación de tokens)
    // 1 token ≈ 4 caracteres => 4000 chars ≈ 1000 tokens, 8000 chars ≈ 2000 tokens
    private static final int MAX_TOTAL_CHARS_LLM = 4_000;
    private static final int MAX_TOTAL_CHARS_GUARDADOS = 8_000;
    private static final double CHARS_PER_TOKEN = 4.0;

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
        List<Message> list = cache.get(userId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(new UserMessage(content));
            trim(list);
        }
    }

    public void addAssistantMessage(String userId, String content) {
        List<Message> list = cache.get(userId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(new AssistantMessage(content));
            trim(list);
        }
    }

    /**
     * Devuelve el historial reciente para enviar al LLM.
     - Primero limita por cantidad (últimos 8 mensajes).
     - Luego limita por caracteres totales: si excede ~4.000 chars, descarta los más antiguos.
     * Esto previene el problema de "8 mensajes gigantes" = 200k+ tokens.
     */
    public List<Message> getHistory(String userId) {
        List<Message> todos = cache.getIfPresent(userId);
        if (todos == null || todos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> recientes;
        synchronized (todos) {
            if (todos.size() <= MAX_MESSAGES_LLM) {
                recientes = new ArrayList<>(todos);
            } else {
                recientes = new ArrayList<>(todos.subList(todos.size() - MAX_MESSAGES_LLM, todos.size()));
            }
        }

        // Limitar por caracteres totales (más restrictivo que cantidad)
        List<Message> limitadosPorChars = trimByChars(recientes, MAX_TOTAL_CHARS_LLM);

        if (log.isDebugEnabled()) {
            int totalChars = contarCaracteres(limitadosPorChars);
            double estimatedTokens = totalChars / CHARS_PER_TOKEN;
            log.debug("[Memory] Historial para userId={}: {} mensajes, ~{} chars, ~{:.0f} tokens estimados",
                    userId, limitadosPorChars.size(), totalChars, estimatedTokens);
        }

        return limitadosPorChars;
    }

    public void clear(String userId) {
        cache.invalidate(userId);
        log.info("[Memory] Conversación reiniciada para userId={}", userId);
    }

    /**
     * Trim por cantidad y por caracteres en la memoria interna.
     */
    private void trim(List<Message> list) {
        if (list == null) return;
        synchronized (list) {
            // Trim por cantidad
            if (list.size() > MAX_MESSAGES_GUARDADOS) {
                list.subList(0, list.size() - MAX_MESSAGES_GUARDADOS).clear();
            }
            // Trim por caracteres (más restrictivo)
            List<Message> trimmed = trimByChars(list, MAX_TOTAL_CHARS_GUARDADOS);
            if (trimmed.size() < list.size()) {
                list.clear();
                list.addAll(trimmed);
            }
        }
    }

    /**
     * Elimina mensajes antiguos hasta que el total de caracteres sea <= maxChars.
     * Nunca elimina el último mensaje (más reciente) para preservar contexto mínimo.
     */
    private List<Message> trimByChars(List<Message> source, int maxChars) {
        int totalChars = contarCaracteres(source);
        if (totalChars <= maxChars || source.size() <= 1) {
            return new ArrayList<>(source);
        }

        List<Message> result = new ArrayList<>(source);
        while (contarCaracteres(result) > maxChars && result.size() > 1) {
            result.remove(0); // Eliminar el más antiguo
        }
        return result;
    }

    private int contarCaracteres(List<Message> messages) {
        if (messages == null) return 0;
        int sum = 0;
        for (Message m : messages) {
            String text = m.getText();
            sum += (text != null) ? text.length() : 0;
        }
        return sum;
    }

    /**
     * Estimación de tokens para logging/métricas.
     * Útil para monitorear el consumo real de contexto.
     */
    public int estimateTokens(String userId) {
        List<Message> historial = getHistory(userId);
        int chars = contarCaracteres(historial);
        return (int) Math.ceil(chars / CHARS_PER_TOKEN);
    }
}
