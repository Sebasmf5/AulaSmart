package co.edu.uceva.chatservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("Eres el asistente virtual oficial de AulaSmart de la Unidad Central del Valle del Cauca (UCEVA). " +
                        "Tu único propósito es ayudar a docentes y estudiantes a consultar disponibilidad de aulas y crear reservas. " +
                        "Si el usuario te pregunta por cualquier otro tema fuera del ámbito de infraestructura física, reservas o aulas, " +
                        "debes negarte amablemente a responder. " +
                        "Debes extraer parámetros como fechas, horas y capacidad de las aulas de la intención del usuario y usar tus herramientas (Tools) para responder.")
                .build();
    }
}