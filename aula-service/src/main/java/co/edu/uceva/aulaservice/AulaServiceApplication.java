package co.edu.uceva.aulaservice;

import co.edu.uceva.security.config.CommonSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"co.edu.uceva.aulaservice", "co.edu.uceva.security"})
@ComponentScan(
    basePackages = {"co.edu.uceva.aulaservice", "co.edu.uceva.security"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CommonSecurityConfig.class
    )
)
@EnableScheduling
public class AulaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AulaServiceApplication.class, args);
	}

}
