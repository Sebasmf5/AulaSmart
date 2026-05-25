package co.edu.uceva.chatservice.config;

import co.edu.uceva.security.filter.EncryptionFilter;
import co.edu.uceva.security.session.InMemorySessionStore;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ChatEncryptionConfig {

    @Bean
    public FilterRegistrationBean<EncryptionFilter> encryptionFilter(InMemorySessionStore sessionStore) {
        FilterRegistrationBean<EncryptionFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new EncryptionFilter(sessionStore));
        reg.addUrlPatterns("/api/v1/chat/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return reg;
    }
}
