package co.edu.uceva.security.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de transporte para payloads cifrados con AES.
 * encryptedData: ciphertext en Base64.
 * sessionId:     identificador de sesión para buscar la llave en Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedPayloadDto {
    private String encryptedData;
    private String sessionId;
}
