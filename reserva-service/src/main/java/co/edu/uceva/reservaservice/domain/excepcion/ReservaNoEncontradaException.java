package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaNoEncontradaException extends RuntimeException {
    public ReservaNoEncontradaException(Long id) {
        super("El producto con id " + id + " no existe.");
    }
}
