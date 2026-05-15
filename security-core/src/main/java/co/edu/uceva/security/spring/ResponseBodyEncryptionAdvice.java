package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AESCBC;
import co.edu.uceva.security.aes.HmacSHA256;
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

/**
 * Interceptor de respuestas salientes que cifra el body con AES-CBC + HMAC-SHA256.
 *
 * Flujo:
 * 1. Serializa la respuesta del controlador a JSON.
 * 2. Genera un IV aleatorio nuevo para esta respuesta.
 * 3. Cifra con AES-128-CBC(key, iv).
 * 4. Calcula HMAC-SHA256(key, iv || ciphertext) para garantizar integridad.
 * 5. Devuelve EncryptedPayloadDto con encryptedData, iv, hmac, sessionId.
 *
 * Endpoints en whitelist (key-exchange, swagger, actuator) pasan sin cifrar.
 */
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

        // Si ya es un DTO encriptado o el DTO de key-exchange → pasar sin procesar
        if (body instanceof EncryptedPayloadDto || body instanceof co.edu.uceva.security.models.KeyExchangeDto) {
            return body;
        }

        // Whitelist de endpoints que responden en texto plano
        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs")
                || path.contains("/swagger-ui")
                || path.contains("/actuator")
                || path.startsWith("/api/v1/crypto/")
                || path.startsWith("/api/v1/auth/")
                || path.equals("/error")) {
            return body;
        }

        try {
            // 1. Serializar respuesta a bytes JSON
            byte[] plainBytes;
            if (body instanceof String) {
                plainBytes = ((String) body).getBytes(StandardCharsets.UTF_8);
            } else {
                plainBytes = objectMapper.writeValueAsBytes(body);
            }

            // 2. Obtener la llave AES y sessionId del contexto actual
            String sessionId = resolveSessionId(request);
            byte[] keyBytes  = encryptionContext.getCurrentKey();
            if (keyBytes == null) {
                keyBytes = sessionKeyStore.getKey(sessionId);
            }

            // 3. Generar IV aleatorio para esta respuesta (nunca reutilizar IV)
            byte[] iv = AESCBC.generateIV();

            // 4. Cifrar con AES-128-CBC
            AESCBC aesCbc = new AESCBC(keyBytes);
            byte[] cipherBytes = aesCbc.encrypt(plainBytes, iv);

            // 5. Calcular HMAC-SHA256 sobre (IV || ciphertext) para autenticar la respuesta
            byte[] hmacInput = concat(iv, cipherBytes);
            byte[] hmacBytes = HmacSHA256.compute(keyBytes, hmacInput);

            // 6. Codificar todo en Base64 y construir el DTO de respuesta
            String encryptedData = Base64.getEncoder().encodeToString(cipherBytes);
            String ivB64         = Base64.getEncoder().encodeToString(iv);
            String hmacB64       = Base64.getEncoder().encodeToString(hmacBytes);

            return new EncryptedPayloadDto(encryptedData, ivB64, hmacB64, sessionId);

        } catch (Exception e) {
            // Si falla el cifrado (ej: no hay llave de sesión o es respuesta de error),
            // responder en texto plano para evitar loop infinito con GlobalExceptionHandler
            return body;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveSessionId(ServerHttpRequest request) {
        String ctxSessionId = encryptionContext.getCurrentSessionId();
        if (ctxSessionId != null && !ctxSessionId.isBlank()) return ctxSessionId;

        String headerSessionId = request.getHeaders().getFirst("X-Session-ID");
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) { }

        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;

        throw new CryptoException("E2E Enforced: No se pudo determinar el sessionId para cifrar la respuesta en " + request.getURI().getPath());
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
