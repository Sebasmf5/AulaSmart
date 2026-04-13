package co.edu.uceva.aulaservice.domain.exception;

public class AulaNoEncontradaException extends RuntimeException {
    public AulaNoEncontradaException(Long id) {
        super("El aula " + id + "no fue encontrada.");
    }
}