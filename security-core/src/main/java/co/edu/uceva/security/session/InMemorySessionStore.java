package co.edu.uceva.security.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore {
    private static final long TTL_MILLIS = 30 * 60 * 1000; // 30 minutos
    // Mapa estatico para garantizar que TODAS las instancias (CryptoController, EncryptionFilter, etc.)
    // compartan las mismas sesiones, sin depender del ciclo de vida de Spring.
    private static final Map<String, CryptoSession> sessions = new ConcurrentHashMap<>();

    public String createSession(byte[] aesKey) {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new CryptoSession(id, aesKey, Instant.now()));
        System.out.println("[InMemorySessionStore] Session created: " + id + " | total sessions: " + sessions.size());
        return id;
    }

    public CryptoSession getSession(String sessionId) {
        CryptoSession s = sessions.get(sessionId);
        System.out.println("[InMemorySessionStore] Session lookup: " + sessionId + " | found: " + (s != null) + " | total sessions: " + sessions.size());
        if (s == null) return null;
        if (Instant.now().toEpochMilli() - s.getCreatedAt().toEpochMilli() > TTL_MILLIS) {
            sessions.remove(sessionId);
            return null;
        }
        return s;
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
