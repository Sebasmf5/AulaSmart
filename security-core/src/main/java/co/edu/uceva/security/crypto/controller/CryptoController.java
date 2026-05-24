package co.edu.uceva.security.crypto.controller;

import co.edu.uceva.security.rsa.RSA;
import co.edu.uceva.security.rsa.RsaKeyStore;
import co.edu.uceva.security.session.InMemorySessionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/crypto")
public class CryptoController {

    private final InMemorySessionStore sessionStore;
    private final RsaKeyStore rsaKeyStore;

    public CryptoController(InMemorySessionStore sessionStore, RsaKeyStore rsaKeyStore) {
        this.sessionStore = sessionStore;
        this.rsaKeyStore = rsaKeyStore;
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        var kp = rsaKeyStore.getKeyPair();
        return ResponseEntity.ok(Map.of(
                "n", kp.getN().toString(16),
                "e", kp.getE().toString(16)
        ));
    }

    @PostMapping("/handshake")
    public ResponseEntity<Map<String, String>> handshake(@RequestBody Map<String, String> body) {
        String encryptedHex = body.get("encryptedAesKey");
        if (encryptedHex == null || encryptedHex.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "encryptedAesKey requerido"));
        }
        try {
            BigInteger cipher = new BigInteger(encryptedHex, 16);
            var kp = rsaKeyStore.getKeyPair();
            BigInteger plain = RSA.decrypt(cipher, kp.getD(), kp.getN());
            byte[] aesKey = plain.toByteArray();

            // Ajustar a exactamente 16 bytes (quitar posible byte de signo o rellenar)
            if (aesKey.length > 16) {
                byte[] t = new byte[16];
                System.arraycopy(aesKey, aesKey.length - 16, t, 0, 16);
                aesKey = t;
            } else if (aesKey.length < 16) {
                byte[] t = new byte[16];
                System.arraycopy(aesKey, 0, t, 16 - aesKey.length, aesKey.length);
                aesKey = t;
            }

            String sessionId = sessionStore.createSession(aesKey);
            return ResponseEntity.ok(Map.of("sessionId", sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
