package co.edu.uceva.security.redis;

import co.edu.uceva.security.config.exceptions.CryptoException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona la persistencia y recuperación de llaves AES en memoria (sin Redis).
 *
 * <p>Las llaves se almacenan en memoria para evitar la dependencia de Redis.</p>
 */
@Component
public class SessionKeyStore {

    private final Map<String, String> keyStore = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Persistencia
    // -------------------------------------------------------------------------

    /**
     * Guarda la llave AES (raw bytes) asociada al sessionId.
     *
     * @param sessionId identificador de sesión
     * @param keyBytes  llave AES de 16 bytes
     */
    public void storeKey(String sessionId, byte[] keyBytes) {
        if (keyBytes.length != 16) {
            throw new CryptoException("La llave AES debe tener exactamente 16 bytes.");
        }
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        keyStore.put(sessionId, encoded);
    }

    /**
     * Sobrecarga: guarda la llave. El TTL es ignorado en esta implementación en memoria.
     */
    public void storeKey(String sessionId, byte[] keyBytes, Duration ttl) {
        storeKey(sessionId, keyBytes);
    }

    // -------------------------------------------------------------------------
    // Recuperación
    // -------------------------------------------------------------------------

    /**
     * Recupera la llave AES (raw bytes) para el sessionId dado.
     *
     * @param sessionId identificador de sesión
     * @return llave AES de 16 bytes
     * @throws CryptoException si el sessionId no tiene llave registrada
     */
    public byte[] getKey(String sessionId) {
        String encoded = keyStore.get(sessionId);
        if (encoded == null) {
            throw new CryptoException("No se encontró llave AES para la sesión: " + sessionId);
        }
        return Base64.getDecoder().decode(encoded);
    }

    // -------------------------------------------------------------------------
    // Eliminación
    // -------------------------------------------------------------------------

    /**
     * Elimina la llave AES asociada al sessionId (útil para invalidar sesiones).
     */
    public void deleteKey(String sessionId) {
        keyStore.remove(sessionId);
    }
}
