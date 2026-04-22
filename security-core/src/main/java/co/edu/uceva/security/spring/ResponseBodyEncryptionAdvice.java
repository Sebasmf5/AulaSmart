package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.EncryptedPayloadDto;
import co.edu.uceva.security.protocol.EncryptionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Intercepta respuestas de controladores que deben ser cifradas.
 *
 * <p>Flujo:<br>
 * 1. Comprueba que {@link EncryptionContext} tenga una llave activa
 *    (sólo cuando la petición fue descifrada previamente).<br>
 * 2. Serializa el body original a JSON.<br>
 * 3. Cifra con {@code AES} usando la llave del contexto de petición.<br>
 * 4. Envuelve el resultado en {@link EncryptedPayloadDto} con el sessionId
 *    extraído del header {@code X-Session-ID} de la petición.</p>
 */
@RestControllerAdvice
public class ResponseBodyEncryptionAdvice implements ResponseBodyAdvice<Object> {

    private final EncryptionContext encryptionContext;
    private final ObjectMapper      objectMapper;

    public ResponseBodyEncryptionAdvice(EncryptionContext encryptionContext,
                                        ObjectMapper objectMapper) {
        this.encryptionContext = encryptionContext;
        this.objectMapper      = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Control: actúa sólo cuando hay una llave de sesión activa
    // -------------------------------------------------------------------------

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Se activa cuando el EncryptionContext tiene una llave (i.e., la request fue cifrada)
        return encryptionContext.hasKey();
    }

    // -------------------------------------------------------------------------
    // Cifrado de la respuesta
    // -------------------------------------------------------------------------

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // No re-envolver si ya es un EncryptedPayloadDto
        if (body instanceof EncryptedPayloadDto) {
            return body;
        }

        try {
            // 1. Serializar response body a JSON
            byte[] plainBytes = objectMapper.writeValueAsBytes(body);

            // 2. Cifrar con AES usando la llave del contexto de petición
            byte[] keyBytes    = encryptionContext.getCurrentKey();
            AES    aes         = new AES(keyBytes);
            byte[] cipherBytes = aes.encrypt(plainBytes);

            // 3. Codificar ciphertext en Base64
            String encryptedData = Base64.getEncoder().encodeToString(cipherBytes);

            // 4. Obtener sessionId del header de la petición
            String sessionId = request.getHeaders().getFirst("X-Session-ID");

            return new EncryptedPayloadDto(encryptedData, sessionId);

        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CryptoException("Error al cifrar el cuerpo de la respuesta.", e);
        }
    }
}
