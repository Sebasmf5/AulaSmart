package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.EncryptedPayloadDto;
import co.edu.uceva.security.protocol.EncryptionContext;
import co.edu.uceva.security.redis.SessionKeyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestControllerAdvice
public class RequestBodyDecryptionAdvice extends RequestBodyAdviceAdapter {

    private final SessionKeyStore   sessionKeyStore;
    private final EncryptionContext encryptionContext;
    private final ObjectMapper      objectMapper;

    @Autowired
    private HttpServletRequest httpRequest;

    public RequestBodyDecryptionAdvice(SessionKeyStore sessionKeyStore,
                                       EncryptionContext encryptionContext,
                                       ObjectMapper objectMapper) {
        this.sessionKeyStore   = sessionKeyStore;
        this.encryptionContext = encryptionContext;
        this.objectMapper      = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String bodyString = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);

        String path = httpRequest.getRequestURI();

        // Endpoints whitelist que pueden ir en texto plano (como el key-exchange o swagger)
        if (path.contains("/crypto/key-exchange") || path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator")) {
            return new CustomHttpInputMessage(bodyString.getBytes(StandardCharsets.UTF_8), inputMessage.getHeaders());
        }

        if (bodyString.trim().startsWith("{") && bodyString.contains("\"encryptedData\"")) {
            try {
                EncryptedPayloadDto dto = objectMapper.readValue(bodyString, EncryptedPayloadDto.class);
                
                if (dto.getEncryptedData() != null) {
                    String headerSessionId = inputMessage.getHeaders().getFirst("X-Session-ID");
                    String sessionId = resolveSessionId(headerSessionId, dto.getSessionId());

                    byte[] keyBytes = sessionKeyStore.getKey(sessionId);
                    byte[] cipherBytes = Base64.getDecoder().decode(dto.getEncryptedData());
                    AES aes = new AES(keyBytes);
                    byte[] plainBytes = aes.decrypt(cipherBytes);

                    encryptionContext.setCurrentKey(keyBytes);
                    encryptionContext.setCurrentSessionId(sessionId);

                    return new CustomHttpInputMessage(plainBytes, inputMessage.getHeaders());
                }
            } catch (Exception e) {
                throw new CryptoException("Error al descifrar el payload E2E: " + e.getMessage());
            }
        }

        // Si llega a este punto es porque el cliente envió JSON en Plain Text en lugar del DTO cifrado.
        // RECHAZAMOS para forzar que NADA entre en texto plano.
        throw new CryptoException("E2E Enforced: La petición a " + path + " debe viajar encriptada. Plaintext JSON no está permitido por seguridad.");
    }

    private String resolveSessionId(String headerSessionId, String dtoSessionId) {
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) {
        }
        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;
        if (dtoSessionId    != null && !dtoSessionId.isBlank())    return dtoSessionId;
        throw new CryptoException("sessionId no disponible: falta JWT o header X-Session-ID.");
    }

    private static class CustomHttpInputMessage implements HttpInputMessage {
        private final byte[] body;
        private final HttpHeaders headers;

        public CustomHttpInputMessage(byte[] body, HttpHeaders headers) {
            this.body = body;
            this.headers = headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
