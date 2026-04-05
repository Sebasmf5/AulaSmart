package co.edu.uceva.incidenciaservice.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Implementación del servicio de IA que delega en OpenAI (gpt-4o-mini, via Spring AI)
 * para generar cartas formales de incidencias en el sistema AulaSmart.
 *
 * <p>La llamada es síncrona: el endpoint de creación de incidencias espera la respuesta
 * de la IA antes de persistir. Si OpenAI falla (timeout, rate-limit, etc.) la
 * incidencia se guarda igualmente con {@code cartaFormalGenerada = null}.</p>
 */
@Service
public class ChatGPTCartaServiceImpl implements ChatGPTCartaService {

    private static final Logger log = LoggerFactory.getLogger(ChatGPTCartaServiceImpl.class);

    private final ChatClient chatClient;

    // Spring AI autoconfigura el ChatClient.Builder con la API key del application.properties
    public ChatGPTCartaServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generarCartaFormal(String descripcionBreve, String tipoIncidencia, Long codigoAula) {
        try {
            String prompt = construirPrompt(descripcionBreve, tipoIncidencia, codigoAula);

            String cartaGenerada = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("[OpenAI] Carta formal generada exitosamente para aula {} - tipo: {}", codigoAula, tipoIncidencia);
            return cartaGenerada;

        } catch (Exception e) {
            log.error("[OpenAI] Error al generar carta formal para aula {}: {}", codigoAula, e.getMessage());
            // Fallback graceful: la incidencia se guardará sin carta
            return null;
        }
    }

    /**
     * Construye el prompt contextualizado con los datos de la incidencia.
     * El prompt está diseñado para que la IA devuelva únicamente el cuerpo de la carta.
     */
    private String construirPrompt(String descripcionBreve, String tipoIncidencia, Long codigoAula) {
        String fechaActual = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO")));

        String tipoLegible = switch (tipoIncidencia) {
            case "QUEJA"          -> "Queja";
            case "RECLAMO"        -> "Reclamo";
            case "RECOMENDACION"  -> "Recomendación";
            case "DANO_FISICO"    -> "Daño Físico";
            default               -> tipoIncidencia;
        };

        return """
                Eres el asistente oficial del sistema AulaSmart de la Unidad Central del Valle del Cauca (UCEVA) (Tulua, Colombia).
                Tu función es redactar cartas formales para reportar incidencias en aulas universitarias.
                
                Con base en los siguientes puntos clave reportados por un usuario, redacta una carta formal,
                respetuosa y completa en español, dirigida al Administrador del Sistema AulaSmart
                o al responsable del área de mantenimiento, según corresponda.
                
                Datos de la incidencia:
                - Fecha del reporte: %s
                - Tipo de incidencia: %s
                - Aula afectada (código): %s
                - Descripción breve del usuario: "%s"
                
                Instrucciones para redactar la carta:
                1. Encabezado formal: ciudad, fecha, destinatario (Administrador del Sistema AulaSmart / Jefe de Mantenimiento / Servicios generales).
                2. Cuerpo: descripción clara y formal del problema, expandiendo los puntos clave del usuario con lenguaje apropiado.
                3. Solicitud concreta y respetuosa de atención o revisión.
                4. Cierre cordial con firma: "Sistema AulaSmart - UCEVA".
                IMPORTANTE: Responde ÚNICAMENTE con el texto de la carta. No agregues explicaciones, comentarios ni texto adicional fuera de la carta.
                """.formatted(fechaActual, tipoLegible, codigoAula, descripcionBreve);
    }
}
