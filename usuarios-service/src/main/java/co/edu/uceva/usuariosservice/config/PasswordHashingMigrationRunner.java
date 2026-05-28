package co.edu.uceva.usuariosservice.config;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migra contraseñas en texto plano a BCrypt al iniciar la aplicación.
 * Solo hashea las contraseñas que NO empiezan con '$2a$' (prefijo de BCrypt).
 */
@Component
public class PasswordHashingMigrationRunner implements CommandLineRunner {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordHashingMigrationRunner(IUsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        int migrados = 0;

        for (Usuario usuario : usuarios) {
            String password = usuario.getPassword();
            if (password != null && !password.isBlank() && !password.startsWith("$2a$")) {
                String hashed = passwordEncoder.encode(password);
                usuario.setPassword(hashed);
                usuarioRepository.save(usuario);
                migrados++;
                System.out.println("[PasswordMigration] Contraseña hasheada para usuario: " + usuario.getEmail());
            }
        }

        if (migrados > 0) {
            System.out.println("[PasswordMigration] " + migrados + " contraseña(s) migrada(s) a BCrypt exitosamente.");
        } else {
            System.out.println("[PasswordMigration] No se encontraron contraseñas en texto plano. Todas están seguras.");
        }
    }
}
