package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaExistenteException extends RuntimeException {
    public ReservaExistenteException(String message) {
        super(message);
    }
}
