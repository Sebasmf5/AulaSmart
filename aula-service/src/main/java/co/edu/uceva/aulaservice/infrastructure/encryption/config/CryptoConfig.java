package co.edu.uceva.aulaservice.infrastructure.encryption.config;

import co.edu.uceva.security.protocol.KeyExchangeService;
import co.edu.uceva.security.redis.SessionKeyStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * Configuración de los beans de cifrado para aula-service.
 *
 * <h3>Propiedades requeridas (application.properties):</h3>
 * <pre>
 * crypto.rsa.private-d=${RSA_MASTER_D}
 * crypto.rsa.public-n=${RSA_MASTER_N}
 * crypto.db.master-key=${DB_MASTER_KEY}
 * </pre>
 */
@Configuration
public class CryptoConfig {

    @Value("${crypto.rsa.private-d}")
    private String rsaPrivateD;

    @Value("${crypto.rsa.public-n}")
    private String rsaPublicN;

    @Bean
    public KeyExchangeService keyExchangeService(SessionKeyStore sessionKeyStore) {
        return new KeyExchangeService(
                new BigInteger(rsaPrivateD, 16),
                new BigInteger(rsaPublicN, 16),
                sessionKeyStore
        );
    }
}
