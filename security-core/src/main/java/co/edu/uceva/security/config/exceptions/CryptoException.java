package co.edu.uceva.security.config.exceptions;

/**
 * Excepción de runtime para fallos criptográficos (cifrado / descifrado / intercambio de llave).
 */
public class CryptoException extends RuntimeException {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
