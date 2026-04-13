package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaNoPermitidaException extends RuntimeException {
  public ReservaNoPermitidaException() {
    super("El tipo de aula recibido no es válido para reservas de Estudiante.");
  }
}
