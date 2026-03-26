package co.edu.uceva.aulaservice.domain.exception;

public class AulaExistenteException extends RuntimeException {
    public AulaExistenteException(String nombre) {
        super("La aula con nombre '" + nombre + "' ya existe.");
    }
}
