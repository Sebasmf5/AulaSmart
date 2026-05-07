package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AES;
import co.edu.uceva.security.config.exceptions.CryptoException;
import co.edu.uceva.security.models.EncryptedPayloadDto;
import co.edu.uceva.security.protocol.EncryptionContext;
import co.edu.uceva.security.redis.SessionKeyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Intercepta peticiones para descifrar el cuerpo si viene como {@link EncryptedPayloadDto}.
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

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Soportar todo. Verificaremos el contenido en beforeBodyRead.
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String bodyString = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);

        // Chequeo rápido para ver si parece un EncryptedPayloadDto
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
                // Si falla, tal vez no era un EncryptedPayloadDto real, procesar como original
            }
        }

        return new CustomHttpInputMessage(bodyString.getBytes(StandardCharsets.UTF_8), inputMessage.getHeaders());
    }

    private String resolveSessionId(String headerSessionId, String dtoSessionId) {
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) {
            // No hay JWT autenticado, continuar con fallbacks
        }
        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;
        if (dtoSessionId    != null && !dtoSessionId.isBlank())    return dtoSessionId;
        throw new CryptoException(
                "sessionId no disponible: falta JWT con claim sessionId, " +
                "header X-Session-ID y campo sessionId en el DTO.");
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
