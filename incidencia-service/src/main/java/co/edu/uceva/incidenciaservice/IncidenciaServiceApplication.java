package co.edu.uceva.incidenciaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"co.edu.uceva.incidenciaservice", "co.edu.uceva.security"})
public class IncidenciaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciaServiceApplication.class, args);
    }

}
