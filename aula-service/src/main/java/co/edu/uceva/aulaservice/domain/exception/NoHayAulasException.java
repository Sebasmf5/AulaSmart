package co.edu.uceva.aulaservice.domain.exception;

public class NoHayAulasException extends RuntimeException {
    public NoHayAulasException() {
        super("No hay aulas en la base de datos.");
    }
}