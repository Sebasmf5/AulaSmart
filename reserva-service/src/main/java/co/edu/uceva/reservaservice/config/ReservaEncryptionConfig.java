package co.edu.uceva.reservaservice.config;

import co.edu.uceva.security.filter.EncryptionFilter;
import co.edu.uceva.security.session.InMemorySessionStore;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ReservaEncryptionConfig {

    @Bean
    public FilterRegistrationBean<EncryptionFilter> encryptionFilter(InMemorySessionStore sessionStore) {
        FilterRegistrationBean<EncryptionFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new EncryptionFilter(sessionStore));
        reg.addUrlPatterns("/api/v1/reserva-service/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return reg;
    }
}
