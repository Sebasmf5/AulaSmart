package co.edu.uceva.aulaservice.infrastructure.encryption.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Configuración de seguridad para aula-service.
 *
 * <p>Reglas de acceso:</p>
 * <ul>
 *   <li>{@code /api/v1/crypto/**} — público (sin JWT) para el key exchange previo al login.</li>
 *   <li>Cualquier otra ruta requiere autenticación.</li>
 * </ul>
 *
 * <p><strong>Nota:</strong> Si este servicio ya tiene su propio {@code JwtAuthFilter},
 * añadirlo con {@code .addFilterBefore(...)} siguiendo el patrón de usuarios-service.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/v1/crypto/**", "/api/v1/aula-service/**").permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        return http.build();
    }
}
