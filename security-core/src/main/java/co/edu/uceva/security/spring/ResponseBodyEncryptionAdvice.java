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
 * Intercepta respuestas de controladores y las cifra cuando hay una sesión activa.
 *
 * <h3>Resolución del sessionId (por prioridad):</h3>
 * <ol>
 *   <li>{@link EncryptionContext#getCurrentSessionId()} (poblado por el advice de request).</li>
 *   <li>Claim del JWT en {@code SecurityContextHolder} vía {@link JwtSessionIdExtractor}.</li>
 *   <li>Header {@code X-Session-ID} de la petición.</li>
 * </ol>
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

            // 4. Resolver sessionId: contexto → JWT → header
            String sessionId = resolveSessionId(request);

            return new EncryptedPayloadDto(encryptedData, sessionId);

        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CryptoException("Error al cifrar el cuerpo de la respuesta.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveSessionId(ServerHttpRequest request) {
        // Prioridad 1: ya está en el EncryptionContext (lo puso el DecryptionAdvice)
        String ctxSessionId = encryptionContext.getCurrentSessionId();
        if (ctxSessionId != null && !ctxSessionId.isBlank()) return ctxSessionId;

        // Prioridad 2: JWT en SecurityContextHolder
        String headerSessionId = request.getHeaders().getFirst("X-Session-ID");
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) {
            // No hay JWT autenticado
        }

        // Prioridad 3: header HTTP
        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;

        throw new CryptoException("No se pudo determinar el sessionId para la respuesta.");
    }
}
