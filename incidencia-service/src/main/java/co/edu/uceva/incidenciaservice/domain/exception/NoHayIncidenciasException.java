package co.edu.uceva.incidenciaservice.domain.exception;

public class NoHayIncidenciasException extends RuntimeException {
    public NoHayIncidenciasException() {
        super("No existen incidencias en el sistema en este momento.");
    }
}
