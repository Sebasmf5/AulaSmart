package co.edu.uceva.security.rsa;

import org.springframework.stereotype.Component;

@Component
public class RsaKeyStore {
    private final RSAKeyPair keyPair;

    public RsaKeyStore() {
        // 512 bits por primo => n ~ 1024 bits, suficiente para AES-128
        this.keyPair = RSA.generateKeyPair(512);
    }

    public RSAKeyPair getKeyPair() {
        return keyPair;
    }
}
