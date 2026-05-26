package co.edu.uceva.security.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySessionStore {
    private static final long TTL_MILLIS = 30 * 60 * 1000; // 30 minutos
    private final Map<String, CryptoSession> sessions = new ConcurrentHashMap<>();

    public String createSession(byte[] aesKey) {
        String id = UUID.randomUUID().toString();
        sessions.put(id, new CryptoSession(id, aesKey, Instant.now()));
        System.out.println("[InMemorySessionStore] Session created: " + id + " | store instance: " + System.identityHashCode(this) + " | total sessions: " + sessions.size());
        return id;
    }

    public CryptoSession getSession(String sessionId) {
        CryptoSession s = sessions.get(sessionId);
        System.out.println("[InMemorySessionStore] Session lookup: " + sessionId + " | store instance: " + System.identityHashCode(this) + " | found: " + (s != null) + " | total sessions: " + sessions.size());
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
