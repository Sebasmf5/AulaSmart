package co.edu.uceva.reservaservice.domain.excepcion;

public class ReservaSolapadaException extends RuntimeException {
    public ReservaSolapadaException() {
        super("El horario que acabas solicitar se encuentra ya reservado");
    }
}
