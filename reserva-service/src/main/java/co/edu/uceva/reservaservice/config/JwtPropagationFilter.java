package co.edu.uceva.reservaservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro que captura el token JWT del header Authorization al inicio de cada request
 * y lo almacena en un ThreadLocal. Esto garantiza que el JWT este disponible
 * para los Feign Clients incluso cuando se ejecutan en hilos secundarios.
 */
@Component
public class JwtPropagationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    AuthContext.setJwt(authHeader);
                }
            }
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
