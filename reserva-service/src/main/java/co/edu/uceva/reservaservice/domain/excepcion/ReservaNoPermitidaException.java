package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaNoPermitidaException extends RuntimeException {
  public ReservaNoPermitidaException(String mensaje) {
    super(mensaje);
  }
}
