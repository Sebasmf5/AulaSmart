package co.edu.uceva.security.redis;

import co.edu.uceva.security.config.exceptions.CryptoException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;

/**
 * Gestiona la persistencia y recuperación de llaves AES en Redis.
 *
 * <p>Las llaves se almacenan en Base64 bajo la clave "{sessionId}:aes-key"
 * con un TTL configurable (por defecto 30 minutos).</p>
 */
@Component
public class SessionKeyStore {

    private static final String KEY_PREFIX  = "session:aes:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public SessionKeyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
        redisTemplate.opsForValue().set(redisKey(sessionId), encoded, DEFAULT_TTL);
    }

    /**
     * Sobrecarga: guarda la llave con un TTL personalizado.
     */
    public void storeKey(String sessionId, byte[] keyBytes, Duration ttl) {
        if (keyBytes.length != 16) {
            throw new CryptoException("La llave AES debe tener exactamente 16 bytes.");
        }
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        redisTemplate.opsForValue().set(redisKey(sessionId), encoded, ttl);
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
        String encoded = redisTemplate.opsForValue().get(redisKey(sessionId));
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
        redisTemplate.delete(redisKey(sessionId));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String redisKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
