package co.edu.uceva.security.protocol;

import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.KeyExchangeDto;
import co.edu.uceva.security.redis.SessionKeyStore;
import co.edu.uceva.security.rsa.RSA;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Base64;
import java.util.UUID;

/**
 * Servicio de intercambio de llave.
 *
 * <p>Flujo:<br>
 * 1. El cliente cifra 16 bytes de llave AES con la clave pública RSA del servidor.<br>
 * 2. Este servicio descifra el bloque RSA para recuperar los bytes de la llave AES.<br>
 * 3. La llave se persiste en Redis vía {@link SessionKeyStore}.<br>
 * 4. Se devuelve el sessionId que el cliente debe incluir en el header
 *    {@code X-Session-ID} de cada petición posterior.</p>
 *
 * <p>El número RSA cifrado se transporta en Base64 (big-endian, sin signo).</p>
 */
@Service
public class KeyExchangeService {

    private final SessionKeyStore sessionKeyStore;

    /** Clave privada RSA d */
    private final BigInteger privateKeyD;

    /** Módulo RSA n */
    private final BigInteger publicKeyN;

    /**
     * Constructor que recibe las componentes de la clave privada RSA.
     *
     * @param privateKeyD exponente privado d
     * @param publicKeyN  módulo n
     * @param sessionKeyStore store de sesiones Redis
     */
    public KeyExchangeService(BigInteger privateKeyD,
                              BigInteger publicKeyN,
                              SessionKeyStore sessionKeyStore) {
        this.privateKeyD  = privateKeyD;
        this.publicKeyN   = publicKeyN;
        this.sessionKeyStore = sessionKeyStore;
    }

    /**
     * Procesa un {@link KeyExchangeDto}: descifra la llave AES y la almacena en Redis.
     *
     * @param dto  contiene la llave AES cifrada con RSA en Base64
     * @return sessionId generado para esta sesión
     * @throws CryptoException si el descifrado falla o la llave resultante no tiene 16 bytes
     */
    public String processKeyExchange(KeyExchangeDto dto) {
        try {
            // 1. Decodificar el ciphertext RSA de Base64 a BigInteger
            byte[] cipherBytes = Base64.getDecoder().decode(dto.getEncryptedAesKey());
            BigInteger cipherInt = new BigInteger(1, cipherBytes); // sin signo

            // 2. Descifrar con RSA: aesKeyInt = cipherInt^d mod n
            BigInteger aesKeyInt = RSA.decrypt(cipherInt, privateKeyD, publicKeyN);

            // 3. Convertir BigInteger a byte[] de exactamente 16 bytes
            byte[] aesKeyBytes = toFixedLength(aesKeyInt, 16);

            // 4. Persistir en Redis y generar sessionId
            String sessionId = UUID.randomUUID().toString();
            sessionKeyStore.storeKey(sessionId, aesKeyBytes);

            return sessionId;

        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CryptoException("Error en el intercambio de llave RSA/AES.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Convierte un BigInteger a un arreglo de bytes de longitud fija, rellenando
     * con ceros a la izquierda si es necesario, o truncando bytes de signo superiores.
     */
    private byte[] toFixedLength(BigInteger value, int length) {
        byte[] raw = value.toByteArray();

        if (raw.length == length) {
            return raw;
        }

        byte[] fixed = new byte[length];
        if (raw.length > length) {
            // BigInteger puede incluir un byte 0x00 de signo al inicio
            System.arraycopy(raw, raw.length - length, fixed, 0, length);
        } else {
            // Rellenar con ceros a la izquierda
            System.arraycopy(raw, 0, fixed, length - raw.length, raw.length);
        }
        return fixed;
    }
}
