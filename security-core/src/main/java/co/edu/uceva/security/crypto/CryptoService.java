package co.edu.uceva.security.crypto;

import co.edu.uceva.security.aes.AES;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoService {
    private final AES aes;

    public CryptoService(byte[] key) {
        this.aes = new AES(key);
    }

    public String encrypt(String plaintext) {
        byte[] encrypted = aes.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String base64Cipher) {
        byte[] encrypted = Base64.getDecoder().decode(base64Cipher);
        byte[] decrypted = aes.decrypt(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
