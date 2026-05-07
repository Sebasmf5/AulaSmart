package co.edu.uceva.security.protocol;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Bean de ámbito Request que almacena la llave AES activa y el sessionId
 * durante el ciclo de vida de una única petición HTTP.
 *
 * <p>Es poblado por {@code RequestBodyDecryptionAdvice} al inicio de la
 * petición y puede ser leído por cualquier componente inyectado en el
 * mismo hilo de request (e.g., {@code ResponseBodyEncryptionAdvice}).</p>
 */
@Component
@RequestScope
public class EncryptionContext {

    private byte[] currentKey;
    private String currentSessionId;

    // -------------------------------------------------------------------------
    // currentKey
    // -------------------------------------------------------------------------

    public byte[] getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(byte[] currentKey) {
        this.currentKey = currentKey;
    }

    public boolean hasKey() {
        return currentKey != null;
    }

    // -------------------------------------------------------------------------
    // currentSessionId
    // -------------------------------------------------------------------------

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String currentSessionId) {
        this.currentSessionId = currentSessionId;
    }
}
