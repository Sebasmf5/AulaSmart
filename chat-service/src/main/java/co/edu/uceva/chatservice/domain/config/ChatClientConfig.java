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
 * Configuración del cliente de IA para el chat-service.
 * Registra el ChatClient con system prompt externo y tools automáticas desde ChatToolsConfig.
 */
@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://opencode.ai/zen/go/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:kimi-k2.6}")
    private String model;

    @Value("${chat.system-prompt}")
    private String systemPrompt;

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
                .defaultSystem(systemPrompt)
                .defaultToolCallbacks(ToolCallbacks.from(chatToolsConfig))
                .build();
    }
}
