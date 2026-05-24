package co.edu.uceva.usuariosservice;

import co.edu.uceva.security.config.CommonSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = {"co.edu.uceva.usuariosservice", "co.edu.uceva.security"})
@ComponentScan(
    basePackages = {"co.edu.uceva.usuariosservice", "co.edu.uceva.security"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CommonSecurityConfig.class
    )
)
public class UsuariosServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(UsuariosServiceApplication.class, args);
    }

}
