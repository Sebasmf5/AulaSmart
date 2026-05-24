package co.edu.uceva.security.session;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionStore {
    private static final long TTL_MILLIS = 30 * 60 * 1000; // 30 minutos
    private final Map<String, CryptoSession> sessions = new ConcurrentHashMap<>();

    public String createSession(byte[] aesKey) {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new CryptoSession(id, aesKey, Instant.now()));
        return id;
    }

    public CryptoSession getSession(String sessionId) {
        CryptoSession s = sessions.get(sessionId);
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
