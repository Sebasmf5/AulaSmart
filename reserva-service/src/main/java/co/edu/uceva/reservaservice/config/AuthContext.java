package co.edu.uceva.reservaservice.config;

/**
 * Contexto de autenticacion basado en ThreadLocal para propagar el JWT
 * a traves de hilos secundarios donde el RequestContextHolder
 * puede no tener el ServletRequestAttributes disponible.
 *
 * InheritableThreadLocal propaga el JWT a hilos hijos creados desde
 * el hilo del request, incluyendo parallelStream y executors internos.
 */
public class AuthContext {

    private static final InheritableThreadLocal<String> JWT_HOLDER = new InheritableThreadLocal<>();

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
