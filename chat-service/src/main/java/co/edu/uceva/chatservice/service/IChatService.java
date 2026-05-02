package co.edu.uceva.chatservice.service;

public interface IChatService {
    /**
     * Procesa una consulta en lenguaje natural del usuario y devuelve la respuesta del LLM.
     * @param mensaje El texto enviado por el usuario desde la app móvil.
     * @return La respuesta generada por la IA.
     */
    String procesarMensaje(String mensaje);
}
