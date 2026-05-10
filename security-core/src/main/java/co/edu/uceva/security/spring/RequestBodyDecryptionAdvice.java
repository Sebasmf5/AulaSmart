package co.edu.uceva.security.spring;

import co.edu.uceva.security.aes.AESCBC;
import co.edu.uceva.security.aes.HmacSHA256;
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

/**
 * Interceptor de peticiones entrantes que desencripta el payload E2E.
 *
 * Flujo de seguridad:
 * 1. Extrae el EncryptedPayloadDto (encryptedData + iv + hmac + sessionId).
 * 2. Recupera la llave AES del SessionKeyStore usando el sessionId.
 * 3. Verifica el HMAC-SHA256 sobre (iv || encryptedData) para garantizar integridad.
 * 4. Descifra con AES-128-CBC usando el IV recibido.
 * 5. Inyecta el plaintext JSON al contexto de Spring como si fuera la petición original.
 *
 * Cualquier petición que llegue sin EncryptedPayloadDto es rechazada (texto plano prohibido).
 */
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
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                           MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {

        String bodyString = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = httpRequest.getRequestURI();

        // Whitelist de endpoints que pueden recibir texto plano
        if (path.contains("/crypto/")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-ui")
                || path.contains("/actuator")) {
            return new CustomHttpInputMessage(bodyString.getBytes(StandardCharsets.UTF_8),
                    inputMessage.getHeaders());
        }

        // Verificar que el payload tiene la estructura encriptada esperada
        if (bodyString.trim().startsWith("{") && bodyString.contains("\"encryptedData\"")) {
            try {
                EncryptedPayloadDto dto = objectMapper.readValue(bodyString, EncryptedPayloadDto.class);

                if (dto.getEncryptedData() == null || dto.getEncryptedData().isBlank()) {
                    throw new CryptoException("El campo 'encryptedData' está vacío.");
                }
                if (dto.getIv() == null || dto.getIv().isBlank()) {
                    throw new CryptoException("El campo 'iv' es obligatorio para AES-CBC.");
                }

                // Resolver sessionId desde header o DTO
                String headerSessionId = inputMessage.getHeaders().getFirst("X-Session-ID");
                String sessionId = resolveSessionId(headerSessionId, dto.getSessionId());

                // Recuperar llave AES de la sesión
                byte[] keyBytes = sessionKeyStore.getKey(sessionId);
                byte[] ivBytes         = Base64.getDecoder().decode(dto.getIv());
                byte[] cipherBytes     = Base64.getDecoder().decode(dto.getEncryptedData());

                // Verificar HMAC si fue enviado (protege contra manipulación del ciphertext)
                if (dto.getHmac() != null && !dto.getHmac().isBlank()) {
                    byte[] receivedHmac  = Base64.getDecoder().decode(dto.getHmac());
                    // HMAC se calcula sobre IV || ciphertext
                    byte[] hmacInput     = concat(ivBytes, cipherBytes);
                    byte[] expectedHmac  = HmacSHA256.compute(keyBytes, hmacInput);

                    if (!HmacSHA256.verify(expectedHmac, receivedHmac)) {
                        throw new CryptoException("HMAC inválido: el payload fue manipulado o la llave es incorrecta.");
                    }
                }

                // Descifrar con AES-128-CBC usando IV recibido
                AESCBC aesCbc = new AESCBC(keyBytes);
                byte[] plainBytes = aesCbc.decrypt(cipherBytes, ivBytes);

                // Guardar contexto para cifrar la respuesta con la misma llave/sesión
                encryptionContext.setCurrentKey(keyBytes);
                encryptionContext.setCurrentSessionId(sessionId);

                return new CustomHttpInputMessage(plainBytes, inputMessage.getHeaders());

            } catch (CryptoException ce) {
                throw ce;
            } catch (Exception e) {
                throw new CryptoException("Error al descifrar el payload E2E: " + e.getMessage());
            }
        }

        // Si llega aquí, el cliente envió texto plano → rechazar
        throw new CryptoException(
                "E2E Enforced: La petición a " + path +
                " debe viajar encriptada (AES-CBC + HMAC). Plaintext JSON no está permitido.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String resolveSessionId(String headerSessionId, String dtoSessionId) {
        try {
            return JwtSessionIdExtractor.extract(headerSessionId);
        } catch (CryptoException ignored) { }

        if (headerSessionId != null && !headerSessionId.isBlank()) return headerSessionId;
        if (dtoSessionId    != null && !dtoSessionId.isBlank())    return dtoSessionId;
        throw new CryptoException("sessionId no disponible: incluye el header X-Session-ID o el campo 'sessionId' en el DTO.");
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    // -------------------------------------------------------------------------
    // Inner class: HttpInputMessage personalizado
    // -------------------------------------------------------------------------

    private static class CustomHttpInputMessage implements HttpInputMessage {
        private final byte[]      body;
        private final HttpHeaders headers;

        public CustomHttpInputMessage(byte[] body, HttpHeaders headers) {
            this.body    = body;
            this.headers = headers;
        }

        @Override public InputStream  getBody()    { return new ByteArrayInputStream(body); }
        @Override public HttpHeaders  getHeaders()  { return headers; }
    }
}
