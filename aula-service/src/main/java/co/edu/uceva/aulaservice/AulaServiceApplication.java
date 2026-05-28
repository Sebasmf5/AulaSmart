package co.edu.uceva.aulaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"co.edu.uceva.aulaservice", "co.edu.uceva.security"})
@EnableScheduling
public class AulaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AulaServiceApplication.class, args);
	}

}
