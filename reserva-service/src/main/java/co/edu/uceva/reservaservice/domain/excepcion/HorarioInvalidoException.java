package co.edu.uceva.reservaservice.domain.excepcion;

public class HorarioInvalidoException extends RuntimeException {
    public HorarioInvalidoException() {
        super("El horario que acabas solicitar se encuentra ya reservado");
    }
}
