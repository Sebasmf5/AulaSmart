package co.edu.uceva.chatservice.domain.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String authorizationHeader = null;

        // Intento 1: Obtener del RequestContextHolder (request HTTP actual)
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        }

        // Intento 2: Fallback al ThreadLocal (para invocaciones de tools de Spring AI)
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            authorizationHeader = AuthContext.getJwt();
        }

        // Si existe y es un Bearer token, inyectarlo en la petición de Feign
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            template.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }
}
