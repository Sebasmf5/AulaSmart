package co.edu.uceva.aulaservice.domain.exception;

public class AulaCodigoNoEncontrada extends RuntimeException {
    public AulaCodigoNoEncontrada(Long codigo) {
        super("El aula con el codigo " + codigo + " no existe.");
    }
}
