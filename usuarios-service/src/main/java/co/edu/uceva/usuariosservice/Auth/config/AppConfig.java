package co.edu.uceva.usuariosservice.Auth.config;

import co.edu.uceva.usuariosservice.domain.excepcions.UsuarioNoEncontradoException;
import lombok.RequiredArgsConstructor;
import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class AppConfig {

    private final IUsuarioRepository usuarioRepository;

    public AppConfig(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return username -> {
            Long codigo = Long.parseLong(username);
            final Usuario usuario = usuarioRepository.findById(codigo)
                    .orElseThrow(() -> new UsuarioNoEncontradoException(codigo));
            return org.springframework.security.core.userdetails.User.builder()
                    .username(codigo.toString())
                    .password(usuario.getPassword())
                    .build();
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cofig) throws Exception{
        return cofig.getAuthenticationManager();
    }
}