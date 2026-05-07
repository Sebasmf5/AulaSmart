package co.edu.uceva.aulaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "co.edu.uceva.aulaservice",
        "co.edu.uceva.security"
})
public class AulaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AulaServiceApplication.class, args);
	}

}
