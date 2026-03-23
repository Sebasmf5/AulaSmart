package co.edu.uceva.reservaservice.domain.excepcion;

public class NoHayReservasException extends RuntimeException {
    public NoHayReservasException() {
        super("No hay reservas en la base de datos. ");
    }
}
