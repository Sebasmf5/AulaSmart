package co.edu.uceva.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.ai.model.openaisdk.autoconfigure.OpenAiSdkEmbeddingAutoConfiguration;

@SpringBootApplication(scanBasePackages = {
		"co.edu.uceva.chatservice",
		"co.edu.uceva.security"
},
		exclude = {OpenAiSdkEmbeddingAutoConfiguration.class})
@EnableFeignClients

public class ChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}
