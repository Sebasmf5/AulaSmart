package co.edu.uceva.security.converter;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Conversor JPA para <strong>encriptación en reposo</strong> de campos String en PostgreSQL.
 *
 * <p>Utiliza la clase manual {@link AES} (AES-128 ECB) con una llave maestra fija
 * inyectada desde la propiedad {@code crypto.db.master-key} (16 bytes en Base64).</p>
 *
 * <h3>Uso en una entidad:</h3>
 * <pre>{@code
 * @Convert(converter = EncryptDatabaseConverter.class)
 * @Column(name = "email")
 * private String email;
 * }</pre>
 *
 * <h3>Propiedad requerida en application.properties:</h3>
 * <pre>
 * # 16 bytes aleatorios en Base64: openssl rand -base64 16
 * crypto.db.master-key=BASE64_DE_16_BYTES_AQUI
 * </pre>
 *
 * <p><strong>Importante:</strong> ECB sin IV es determinístico. Valores idénticos
 * producen el mismo ciphertext. Para búsquedas por hash exacto esto es ventaja;
 * para campos con alta cardinalidad (emails únicos) es aceptable.</p>
 */
@Converter
@Component
public class EncryptDatabaseConverter implements AttributeConverter<String, String> {

    private final byte[] masterKeyBytes;

    /**
     * @param masterKey llave maestra en Base64 (debe decodificar exactamente a 16 bytes).
     */
    public EncryptDatabaseConverter(
            @Value("${crypto.db.master-key}") String masterKey) {
        this.masterKeyBytes = Base64.getDecoder().decode(masterKey);
        if (this.masterKeyBytes.length != 16) {
            throw new CryptoException(
                    "crypto.db.master-key debe decodificar a exactamente 16 bytes (AES-128). " +
                    "Usa: openssl rand -base64 16");
        }
    }

    // -------------------------------------------------------------------------
    // Escritura → BD: cifrar
    // -------------------------------------------------------------------------

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            AES aes = new AES(masterKeyBytes);
            byte[] cipherBytes = aes.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new CryptoException("Error al cifrar campo para BD.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Lectura ← BD: descifrar
    // -------------------------------------------------------------------------

    @Override
    public String convertToEntityAttribute(String cipherBase64) {
        if (cipherBase64 == null) return null;
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(cipherBase64);
            AES aes = new AES(masterKeyBytes);
            byte[] plainBytes = aes.decrypt(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Error al descifrar campo desde BD.", e);
        }
    }
}
