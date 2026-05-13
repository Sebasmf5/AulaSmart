package co.edu.uceva.chatservice.domain.config;

/**
 * Contexto de autenticación basado en ThreadLocal para propagar el JWT
 * a través de invocaciones de tools de Spring AI, donde el RequestContextHolder
 * puede no tener el ServletRequestAttributes disponible.
 */
public class AuthContext {

    private static final ThreadLocal<String> JWT_HOLDER = new ThreadLocal<>();

    public static void setJwt(String jwt) {
        JWT_HOLDER.set(jwt);
    }

    public static String getJwt() {
        return JWT_HOLDER.get();
    }

    public static void clear() {
        JWT_HOLDER.remove();
    }
}
