package co.edu.uceva.reservaservice.domain.excepcion;

public class PaginaSinReservasException extends RuntimeException {
    public PaginaSinReservasException(int page) {
        super("No hay reservas para esta pagina " + page);
    }
}
