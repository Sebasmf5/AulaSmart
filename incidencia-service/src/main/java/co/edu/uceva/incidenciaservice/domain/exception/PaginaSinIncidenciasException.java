package co.edu.uceva.incidenciaservice.domain.exception;

public class PaginaSinIncidenciasException extends RuntimeException {
    public PaginaSinIncidenciasException() {
        super("No hay incidencias para esta pagina: ");
    }
}
