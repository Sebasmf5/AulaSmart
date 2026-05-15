package co.edu.uceva.usuariosservice.config;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final IUsuarioRepository usuarioRepository;

    public DataSeeder(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Seed omitted: database already has {} users", usuarioRepository.count());
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("Sistema");
        admin.setEmail("admin@uceva.edu.co");
        admin.setPassword("Admin123!");
        admin.setRol("Administrativo");
        usuarioRepository.save(admin);

        log.info("Seed user created: codigo={}, password=Admin123!", admin.getCodigo());
    }
}
