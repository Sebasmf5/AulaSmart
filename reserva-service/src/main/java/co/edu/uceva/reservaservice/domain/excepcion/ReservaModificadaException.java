package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaModificadaException extends RuntimeException {
  public ReservaModificadaException(Long idReserva) {
    super("La reserva con ID " + idReserva +
            " fue modificada por otro usuario. Por favor recarga los datos e intenta de nuevo.");
  }
}
