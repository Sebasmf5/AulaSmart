package co.edu.uceva.incidenciaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "co.edu.uceva.incidenciaservice"
})
public class IncidenciaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciaServiceApplication.class, args);
    }

}
