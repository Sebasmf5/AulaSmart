package co.edu.uceva.security.protocol;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Bean de ámbito Request que almacena la llave AES activa durante
 * el ciclo de vida de una única petición HTTP.
 *
 * <p>Es llenado por {@code RequestBodyDecryptionAdvice} al inicio de la
 * petición y puede ser leído por cualquier componente inyectado en el
 * mismo hilo de request (e.g., {@code ResponseBodyEncryptionAdvice}).</p>
 */
@Component
@RequestScope
public class EncryptionContext {

    private byte[] currentKey;

    public byte[] getCurrentKey() {
        return currentKey;
    }

    public void setCurrentKey(byte[] currentKey) {
        this.currentKey = currentKey;
    }

    public boolean hasKey() {
        return currentKey != null;
    }
}
