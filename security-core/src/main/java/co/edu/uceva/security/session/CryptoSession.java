package co.edu.uceva.security.session;

import lombok.Data;
import java.time.Instant;

@Data
public class CryptoSession {
    private final String sessionId;
    private final byte[] aesKey;
    private final Instant createdAt;
}
