package co.edu.uceva.security.crypto.config;

import co.edu.uceva.security.session.InMemorySessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public InMemorySessionStore sessionStore() {
        return new InMemorySessionStore();
    }
}
