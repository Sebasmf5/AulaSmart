package co.edu.uceva.reservaservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            // Extraer el encabezado Authorization del request original
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            // Si existe y es un Bearer token, inyectarlo en la petición de Feign
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                template.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
            }
        }
    }
}