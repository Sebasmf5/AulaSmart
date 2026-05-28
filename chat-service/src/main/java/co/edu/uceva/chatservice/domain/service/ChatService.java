package co.edu.uceva.chatservice.domain.service;

public interface ChatService {
    String procesarMensaje(String mensajeUsuario);
    void reiniciarConversacion();
}