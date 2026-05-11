package co.edu.uceva.chatservice.domain.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openaisdk.OpenAiSdkChatModel;
import org.springframework.ai.openaisdk.OpenAiSdkChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String model;

    @Bean
    @Primary
    public OpenAIClient openAIClient() {
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .build();
    }

    @Bean
    @Primary
    public OpenAIClientAsync openAIClientAsync() {
        String url = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return OpenAIOkHttpClientAsync.builder()
                .apiKey(apiKey)
                .baseUrl(url)
                .build();
    }

    @Bean
    @Primary
    public OpenAiSdkChatModel openAiSdkChatModel(OpenAIClient openAIClient, OpenAIClientAsync openAIClientAsync) {
        return new OpenAiSdkChatModel(
                openAIClient, 
                openAIClientAsync, 
                OpenAiSdkChatOptions.builder().model(model).build()
        );
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatToolsConfig chatToolsConfig) {
        return builder
                .defaultSystem("Eres el asistente virtual oficial de AulaSmart de la Universidad Central del Valle del Cauca (UCEVA). " +
                        "Tu único propósito es ayudar a docentes y estudiantes a consultar disponibilidad de aulas y crear reservas. " +
                        "Si el usuario te pregunta por cualquier otro tema fuera del ámbito de infraestructura física, reservas o aulas, " +
                        "debes negarte amablemente a responder. " +
                        "Debes extraer parámetros como fechas, horas y capacidad de las aulas de la intención del usuario y usar tus herramientas (Tools) para responder.")
                .defaultToolCallbacks(ToolCallbacks.from(chatToolsConfig))
                .build();
    }
}