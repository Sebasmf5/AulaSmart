package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.EncryptedPayloadDto;
import co.edu.uceva.security.protocol.EncryptionContext;
import co.edu.uceva.security.redis.SessionKeyStore;
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

@RestControllerAdvice
public class ResponseBodyEncryptionAdvice implements ResponseBodyAdvice<Object> {

    private final EncryptionContext encryptionContext;
    private final ObjectMapper      objectMapper;
    private final SessionKeyStore   sessionKeyStore;

    public ResponseBodyEncryptionAdvice(EncryptionContext encryptionContext,
                                        ObjectMapper objectMapper,
                                        SessionKeyStore sessionKeyStore) {
        this.encryptionContext = encryptionContext;
        this.objectMapper      = objectMapper;
        this.sessionKeyStore   = sessionKeyStore;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Enforce everywhere to ensure no plain text leaks
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (body == null) return null;

        if (body instanceof EncryptedPayloadDto || body instanceof co.edu.uceva.security.models.KeyExchangeDto) {
            return body;
        }

        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator") || path.contains("/crypto/key-exchange")) {
            return body;
        }

        try {
            byte[] plainBytes;
            if (body instanceof String) {
                plainBytes = ((String) body).getBytes(StandardCharsets.UTF_8);
            } else {
                plainBytes = objectMapper.writeValueAsBytes(body);
            }

            String sessionId = resolveSessionId(request);
            byte[] keyBytes = encryptionContext.getCurrentKey();
            if (keyBytes == null) {
                keyBytes = sessionKeyStore.getKey(sessionId);
            }

            AES aes = new AES(keyBytes);
            byte[] cipherBytes = aes.encrypt(plainBytes);

            String encryptedData = Base64.getEncoder().encodeToString(cipherBytes);
            return new EncryptedPayloadDto(encryptedData, sessionId);

        } catch (CryptoException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CryptoException("Error al cifrar el cuerpo de la respuesta: " + e.getMessage(), e);
        }
    }

    private String resolveSessionId(ServerHttpRequest request) {
        String ctxSessionId = encryptionContext.getCurrentSessionId();
        if (ctxSessionId != null && !ctxSessionId.isBlank()) return ctxSessionId;

        String headerSessionId = request.getHeaders().getFirst("X-Session-ID");
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) { }

        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;

        throw new CryptoException("E2E Enforced: No se pudo determinar el sessionId para la respuesta en " + request.getURI().getPath());
    }
}
