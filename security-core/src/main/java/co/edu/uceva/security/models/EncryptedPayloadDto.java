package co.edu.uceva.security.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para payloads cifrados con AES-CBC + HMAC-SHA256.
 *
 * Campos:
 * - encryptedData : ciphertext en Base64 (AES-128-CBC).
 * - iv            : vector de inicialización aleatorio en Base64 (16 bytes). Único por mensaje.
 * - hmac          : tag de autenticidad HMAC-SHA256 en Base64. Previene manipulación.
 * - sessionId     : identificador de sesión para recuperar la llave AES del store en memoria.
 *
 * El flujo completo es:
 *   1. Cliente genera AES key random + IV random.
 *   2. Cifra payload con AES-CBC(key, iv).
 *   3. Calcula HMAC-SHA256(key, iv || ciphertext).
 *   4. Envía este DTO al backend.
 *   5. Backend recupera key por sessionId, verifica HMAC, descifra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedPayloadDto {
    /** Ciphertext AES-128-CBC en Base64. */
    private String encryptedData;

    /** IV aleatorio (16 bytes) en Base64. Generado por el cliente por cada petición. */
    private String iv;

    /** HMAC-SHA256(aesKey, iv || encryptedData) en Base64. Para verificar integridad. */
    private String hmac;

    /** SessionId para recuperar la llave AES del SessionKeyStore. */
    private String sessionId;

    /**
     * Constructor de compatibilidad para respuestas del servidor (sin HMAC adicional).
     */
    public EncryptedPayloadDto(String encryptedData, String sessionId) {
        this.encryptedData = encryptedData;
        this.sessionId = sessionId;
    }
}
