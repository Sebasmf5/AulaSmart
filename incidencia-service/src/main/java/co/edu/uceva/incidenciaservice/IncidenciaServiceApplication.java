package co.edu.uceva.incidenciaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"co.edu.uceva.incidenciaservice", "co.edu.uceva.security"}, exclude = {
    org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
    org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class
})
public class IncidenciaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciaServiceApplication.class, args);
    }

}
