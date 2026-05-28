package co.edu.uceva.reservaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"co.edu.uceva.reservaservice", "co.edu.uceva.security"})
@EnableFeignClients
public class ReservaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservaServiceApplication.class, args);
	}

}
