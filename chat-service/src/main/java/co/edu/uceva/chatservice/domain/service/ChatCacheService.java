package co.edu.uceva.chatservice.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de caché para respuestas del chatbot.
 * Evita llamadas repetidas al LLM cuando el usuario pregunta cosas similares
 * o cuando hay consultas frecuentes (ej: "aulas disponibles hoy").
 */
@Service
@Slf4j
public class ChatCacheService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final int MAX_CACHE_SIZE = 100;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public String get(String key) {
        cleanExpired();
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("[Cache] HIT para clave: {}", key);
            return entry.value();
        }
        return null;
    }

    public void put(String key, String value) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            // Eliminar entrada más antigua
            cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue((e1, e2) -> e1.timestamp().compareTo(e2.timestamp())))
                    .ifPresent(oldest -> cache.remove(oldest.getKey()));
        }
        cache.put(key, new CacheEntry(value, Instant.now()));
        log.debug("[Cache] PUT para clave: {}", key);
    }

    public void invalidate(String pattern) {
        cache.keySet().removeIf(key -> key.contains(pattern));
        log.info("[Cache] Invalidadas entradas que contienen: {}", pattern);
    }

    public void clear() {
        cache.clear();
        log.info("[Cache] Caché limpiada completamente");
    }

    private void cleanExpired() {
        Instant cutoff = Instant.now().minus(CACHE_TTL);
        cache.entrySet().removeIf(entry -> entry.getValue().timestamp().isBefore(cutoff));
    }

    private record CacheEntry(String value, Instant timestamp) {
        boolean isExpired() {
            return Instant.now().isAfter(timestamp.plus(CACHE_TTL));
        }
    }
}
