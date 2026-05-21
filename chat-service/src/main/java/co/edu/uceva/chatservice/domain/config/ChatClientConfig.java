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

/**
 * Configuración multi-proveedor para el chat-service.
 * Soporta fallback entre proveedores y modelos según disponibilidad y costo.
 */
@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:llama-3.3-70b-versatile}")
    private String model;

    // Configuración alternativa (Ollama local)
    @Value("${spring.ai.ollama.enabled:false}")
    private boolean ollamaEnabled;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.model:llama3.2:latest}")
    private String ollamaModel;

    // Configuración de caché
    @Value("${chat.cache.enabled:true}")
    private boolean cacheEnabled;

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
                .defaultSystem("Eres AulaBot, asistente de reservas de aulas de UCEVA. " +
                        "Solo respondes sobre aulas y reservas. " +
                        "Para consultas de disponibilidad o reservas, DEBES usar tus tools. " +
                        "No inventes datos. Usa el contexto de la conversación para follow-ups. " +
                        "Antes de reservar, pide el motivo/título si no lo tiene.")
                .defaultToolCallbacks(ToolCallbacks.from(chatToolsConfig))
                .defaultToolNames("consultarPorBloque", "consultarPorTipo", "consultarPorNombreAula", "consultarHorariosAula", "reservarAulaTool")
                .build();
    }
}
