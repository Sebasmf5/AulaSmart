package co.edu.uceva.chatservice;

import org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = {
		"co.edu.uceva.chatservice",
		"co.edu.uceva.security"
},
		exclude = {OpenAiSdkEmbeddingAutoConfiguration.class})
@EnableFeignClients
@EnableScheduling

public class ChatServiceApplication {

	@PostConstruct
	public void initSecurityContextStrategy() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}
