package co.edu.uceva.usuariosservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "co.edu.uceva.usuariosservice",
        "co.edu.uceva.security"
})
public class UsuariosServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(UsuariosServiceApplication.class, args);
    }

}
