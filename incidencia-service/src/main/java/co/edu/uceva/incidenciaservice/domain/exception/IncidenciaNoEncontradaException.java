package co.edu.uceva.incidenciaservice.domain.exception;

public class IncidenciaNoEncontradaException extends RuntimeException {
    public IncidenciaNoEncontradaException(Long id) {
        super("No se encontró una incidencia con el ID: " + id);
    }
}
