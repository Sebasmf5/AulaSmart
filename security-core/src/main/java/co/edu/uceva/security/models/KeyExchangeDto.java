package co.edu.uceva.security.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el handshake de intercambio de llave.
 * encryptedAesKey: llave AES de 16 bytes cifrada con la clave pública RSA,
 *                  transportada en Base64.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyExchangeDto {
    private String encryptedAesKey;
}
