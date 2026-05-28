package co.edu.uceva.reservaservice.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtPropagationFilter> jwtPropagationFilterRegistration(JwtPropagationFilter filter) {
        FilterRegistrationBean<JwtPropagationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/v1/*");
        registration.setOrder(1);
        return registration;
    }
}
