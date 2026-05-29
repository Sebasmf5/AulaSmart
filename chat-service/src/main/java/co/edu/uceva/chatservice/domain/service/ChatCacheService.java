package co.edu.uceva.chatservice.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de caché para respuestas del chatbot.
 * Evita llamadas repetidas al LLM cuando el usuario pregunta cosas similares
 * o cuando hay consultas frecuentes (ej: "aulas disponibles hoy").
 *
 * TTL separados:
 *   - Disponibilidad: 0 segundos (NUNCA cachear - la disponibilidad cambia en tiempo real)
 *   - Datos estáticos: 5 minutos (nombres, capacidades, bloques)
 */
@Service
@Slf4j
public class ChatCacheService {

    static final Duration CACHE_TTL_DISPONIBILIDAD = Duration.ZERO;
    static final Duration CACHE_TTL_ESTATICO = Duration.ofMinutes(5);
    private static final int MAX_CACHE_SIZE = 100;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("[Cache] HIT para clave: {}", key);
            return entry.value();
        }
        return null;
    }

    public void put(String key, String value) {
        put(key, value, CACHE_TTL_ESTATICO);
    }

    public void put(String key, String value, Duration ttl) {
        if (cache.size () >= MAX_CACHE_SIZE) {
            // Eliminar entrada más antigua
            cache.entrySet().stream()
                    .min(Map.Entry.comparingByValue((e1, e2) -> e1.timestamp().compareTo(e2.timestamp())))
                    .ifPresent(oldest -> cache.remove(oldest.getKey()));
        }
        cache.put(key, new CacheEntry(value, Instant.now(), ttl));
        log.debug("[Cache] PUT para clave: {} (ttl={}s)", key, ttl.getSeconds());
    }

    public void invalidate(String pattern) {
        int antes = cache.size();
        cache.keySet().removeIf(key -> key.contains(pattern));
        int despues = cache.size();
        log.info("[Cache] Invalidadas {} entradas que contienen: '{}'", (antes - despues), pattern);
    }

    public void clear() {
        cache.clear();
        log.info("[Cache] Caché limpiada completamente");
    }

    /**
     * Limpieza periódica de entradas expiradas cada 60 segundos.
     * Evita acumulación de entradas muertas en memoria.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanExpired() {
        int antes = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int eliminadas = antes - cache.size();
        if (eliminadas > 0) {
            log.debug("[Cache] Limpieza automática: {} entradas expiradas eliminadas", eliminadas);
        }
    }

    private record CacheEntry(String value, Instant timestamp, Duration ttl) {
        boolean isExpired() {
            return Instant.now().isAfter(timestamp.plus(ttl));
        }
    }
}
