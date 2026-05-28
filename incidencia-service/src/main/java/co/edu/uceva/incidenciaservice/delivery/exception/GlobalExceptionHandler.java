package co.edu.uceva.incidenciaservice.delivery.exception;

import co.edu.uceva.incidenciaservice.domain.exception.IncidenciaNoEncontradaException;
import co.edu.uceva.incidenciaservice.domain.exception.NoHayIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.PaginaSinIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.ValidationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR = "error";
    private static final String MENSAJE = "mensaje";
    private static final String STATUS = "status";
    private static final String INCIDENCIAS = "incidencias";

    @ExceptionHandler(NoHayIncidenciasException.class)
    public ResponseEntity<Map<String, Object>> handleNoHayIncidencias(NoHayIncidenciasException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, ex.getMessage());
        response.put(INCIDENCIAS, null); // siempre el mismo campo incluso si está vacío
        return ResponseEntity.status(HttpStatus.OK).body(response); 
    }

    @ExceptionHandler(IncidenciaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleIncidenciaNoEncontrada(IncidenciaNoEncontradaException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(ERROR, ex.getMessage());
        response.put(STATUS, HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PaginaSinIncidenciasException.class)
    public ResponseEntity<Map<String, Object>> handlePaginaSinIncidencias(PaginaSinIncidenciasException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "Error de hardware o base de datos al realizar la consulta.");
        response.put(ERROR, ex.getMessage().concat(": ").concat(ex.getMostSpecificCause().getMessage()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(ERROR, "Error inesperado en el servidor: " + ex.getMessage());
        response.put(STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, ex.getMessage());

        List<String> errores = ex.result.getFieldErrors()
                        .stream().map(error ->  "El campo " + error.getField() + " " + error.getDefaultMessage()).toList();
        response.put(ERROR, errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
