package co.edu.uceva.usuariosservice.domain.excepcions;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(Long codigo) {
        super("El usuario con Codigo " + codigo + " no fue encontrado.");
    }
}