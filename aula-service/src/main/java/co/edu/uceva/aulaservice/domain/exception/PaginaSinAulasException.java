package co.edu.uceva.aulaservice.domain.exception;

public class PaginaSinAulasException extends RuntimeException {
    public PaginaSinAulasException(int page) {
        super("No hay aulas en la página solicitada: " + page);
    }
}