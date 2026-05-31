package co.edu.uceva.reservaservice.config;

public class AuthContext {
    private static final InheritableThreadLocal<String> JWT_HOLDER = new InheritableThreadLocal<>();
    public static void setJwt(String jwt) { JWT_HOLDER.set(jwt); }
    public static String getJwt() { return JWT_HOLDER.get(); }
    public static void clear() { JWT_HOLDER.remove(); }
}
