package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.EncryptedPayloadDto;
import co.edu.uceva.security.protocol.EncryptionContext;
import co.edu.uceva.security.redis.SessionKeyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Intercepta peticiones cuyo cuerpo es {@link EncryptedPayloadDto}.
 *
 * <p>Flujo:<br>
 * 1. Lee el header {@code X-Session-ID}.<br>
 * 2. Recupera la llave AES desde Redis vía {@link SessionKeyStore}.<br>
 * 3. Descifra {@code encryptedData} (Base64 → bytes cifrados → JSON plano).<br>
 * 4. Deserializa el JSON a la clase destino real del controlador.<br>
 * 5. Guarda la llave en {@link EncryptionContext} para que el advice de
 *    respuesta la reutilice sin volver a Redis.</p>
 */
@RestControllerAdvice
public class RequestBodyDecryptionAdvice extends RequestBodyAdviceAdapter {

    private final SessionKeyStore   sessionKeyStore;
    private final EncryptionContext encryptionContext;
    private final ObjectMapper      objectMapper;

    public RequestBodyDecryptionAdvice(SessionKeyStore sessionKeyStore,
                                       EncryptionContext encryptionContext,
                                       ObjectMapper objectMapper) {
        this.sessionKeyStore   = sessionKeyStore;
        this.encryptionContext = encryptionContext;
        this.objectMapper      = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Control: sólo actúa cuando el tipo de parámetro es EncryptedPayloadDto
    // -------------------------------------------------------------------------

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return EncryptedPayloadDto.class.equals(methodParameter.getParameterType());
    }

    // -------------------------------------------------------------------------
    // Post-lectura: descifrar y transformar
    // -------------------------------------------------------------------------

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        if (!(body instanceof EncryptedPayloadDto dto)) {
            return body;
        }

        try {
            // 1. Obtener sessionId del header HTTP
            String sessionId = inputMessage.getHeaders().getFirst("X-Session-ID");
            if (sessionId == null || sessionId.isBlank()) {
                throw new CryptoException("Header X-Session-ID ausente o vacío.");
            }

            // 2. Recuperar llave AES desde Redis
            byte[] keyBytes = sessionKeyStore.getKey(sessionId);

            // 3. Descifrar payload
            byte[] cipherBytes  = Base64.getDecoder().decode(dto.getEncryptedData());
            AES    aes          = new AES(keyBytes);
            byte[] plainBytes   = aes.decrypt(cipherBytes);
            String plainJson    = new String(plainBytes, StandardCharsets.UTF_8);

            // 4. Compartir la llave en el contexto de la petición
            encryptionContext.setCurrentKey(keyBytes);

            // 5. Deserializar JSON al tipo real esperado por el controlador
            Class<?> targetClass = (Class<?>) targetType;
            return objectMapper.readValue(plainJson, targetClass);

        } catch (CryptoException ce) {
            throw ce;
        } catch (IOException e) {
            throw new CryptoException("Error al deserializar el payload descifrado.", e);
        } catch (Exception e) {
            throw new CryptoException("Error en el descifrado del cuerpo de la petición.", e);
        }
    }
}
