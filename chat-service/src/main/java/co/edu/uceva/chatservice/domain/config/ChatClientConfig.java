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
                .defaultSystem(
                        "Eres el asistente virtual oficial de AulaSmart de la Universidad Central del Valle del Cauca (UCEVA). " +
                        "Tu único propósito es ayudar a docentes y estudiantes a consultar disponibilidad de aulas y crear reservas. " +
                        "Si el usuario te pregunta por cualquier otro tema fuera del ámbito de infraestructura física, reservas o aulas, " +
                        "debes negarte amablemente a responder. " +
                        "REGLA CRÍTICA: Si el usuario quiere consultar disponibilidad o hacer una reserva, NO respondas con texto libre. " +
                        "DEBES invocar una de tus herramientas (tools): consultarPorBloque, consultarPorTipo, consultarPorNombreAula, consultarHorariosAula o reservarAulaTool. " +
                        "No inventes resultados. Siempre usa las tools para obtener datos reales del sistema. " +
                        "REGLA DE MEMORIA: Tienes acceso al historial de esta conversación. Si el usuario dice 'qué horario está libre' o 'y qué aulas hay' sin dar detalles, " +
                        "usa el contexto de la conversación anterior (aula, fecha, bloque) para responder sin pedirle que repita la información. " +
                        "Cuando el usuario quiera hacer una reserva, DEBES preguntarle el motivo o título de la reserva si no lo ha proporcionado. " +
                        "Ejemplos de motivos: 'Reunión de proyecto', 'Clase de refuerzo', 'Examen parcial', 'Tutoría'. " +
                        "No invoques la herramienta de reserva sin antes tener un motivo claro."
                )
                .defaultToolCallbacks(ToolCallbacks.from(chatToolsConfig))
                .defaultToolNames("consultarPorBloque", "consultarPorTipo", "consultarPorNombreAula", "consultarHorariosAula", "reservarAulaTool")
                .build();
    }
}