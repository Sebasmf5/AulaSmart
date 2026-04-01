package co.edu.uceva.incidenciaservice.domain.service;

/**
 * Contrato del servicio de inteligencia artificial para la generación
 * de cartas formales a partir de puntos clave de una incidencia.
 * La implementación concreta delega en OpenAI via Spring AI.
 */
public interface ChatGPTCartaService {

    /**
     * Genera una carta formal completa a partir de los puntos clave de la incidencia.
     *
     * @param descripcionBreve Descripción corta provista por el usuario (puntos clave).
     * @param tipoIncidencia   Tipo de incidencia (QUEJA, RECLAMO, RECOMENDACION, DANO_FISICO).
     * @param codigoAula       Código del aula afectada.
     * @return Texto de la carta formal generada por OpenAI, o {@code null} si ocurre un error.
     */
    String generarCartaFormal(String descripcionBreve, String tipoIncidencia, Long codigoAula);
}
