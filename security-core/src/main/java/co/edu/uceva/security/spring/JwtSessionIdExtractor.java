package co.edu.uceva.security.spring;

import co.edu.uceva.security.config.exceptions.CryptoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utilitario estático para extraer el {@code sessionId} del contexto de seguridad.
 *
 * <h3>Estrategia de extracción (por prioridad):</h3>
 * <ol>
 *   <li>Claim {@code sessionId} del JWT — el {@code JwtAuthFilter} de cada microservicio
 *       debe poblarlo en el {@code UsernamePasswordAuthenticationToken} como detalle
 *       o credential. El patrón recomendado es almacenarlo en las
 *       {@code credentials} del token de autenticación.</li>
 *   <li>Fallback: header HTTP {@code X-Session-ID} (pasado como argumento).</li>
 * </ol>
 *
 * <p>Si ninguna fuente produce un sessionId válido, lanza {@link CryptoException}.</p>
 */
public final class JwtSessionIdExtractor {

    private JwtSessionIdExtractor() {}

    /**
     * Extrae el sessionId. Primero intenta obtenerlo del {@link Authentication#getCredentials()},
     * que los filtros JWT deben poblar con el sessionId del claim. Si no está disponible,
     * usa el {@code fallbackSessionId} (típicamente el header {@code X-Session-ID}).
     *
     * @param fallbackSessionId valor del header X-Session-ID, puede ser null
     * @return sessionId no nulo y no vacío
     * @throws CryptoException si no se puede obtener el sessionId por ninguna vía
     */
    public static String extract(String fallbackSessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            // Los filtros JWT deben almacenar el sessionId en credentials
            Object credentials = auth.getCredentials();
            if (credentials instanceof String sessionId && !sessionId.isBlank()) {
                return sessionId;
            }
        }

        // Fallback: header X-Session-ID (útil para endpoints públicos como /key-exchange)
        if (fallbackSessionId != null && !fallbackSessionId.isBlank()) {
            return fallbackSessionId;
        }

        throw new CryptoException(
                "No se pudo determinar el sessionId: el JWT no contiene el claim 'sessionId' " +
                "y el header X-Session-ID está ausente.");
    }

    /**
     * Verifica si la petición actual tiene autenticación JWT activa
     * (útil para decidir si encriptar la respuesta).
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
               && !(auth.getPrincipal() instanceof String s && s.equals("anonymousUser"));
    }
}
