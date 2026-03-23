package co.edu.uceva.aulaservice.domain.exception;

public class AulaNoEncontradaException extends RuntimeException {
    public AulaNoEncontradaException(Long id) {
        super("La aula con ID " + id + " no fue encontrada.");
    }
}