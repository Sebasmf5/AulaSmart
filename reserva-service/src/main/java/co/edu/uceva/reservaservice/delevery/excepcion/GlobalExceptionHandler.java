package co.edu.uceva.reservaservice.delevery.excepcion;

import co.edu.uceva.reservaservice.domain.excepcion.*;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String RESERVA = "reserva";
    private static final String STATUS = "status";

    @ExceptionHandler(NoHayReservasException.class)
    public ResponseEntity<Map<String, Object>> handleNoHayReservasException(NoHayReservasException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(RESERVA, null);
        return ResponseEntity.status(HttpStatus.OK).body(response); //procesado pero lista vacía
    }

    @ExceptionHandler(PaginaSinReservasException.class)
    public ResponseEntity<Map<String, Object>> handleNoHayReservasException(PaginaSinReservasException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaNoEncontradaException(ReservaNoEncontradaException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(ERROR, ex.getMessage());
        //mostrar los campos que tuvieron errores
        List<String> errores = ex.result.getFieldErrors()
                .stream()
                .map(error -> "El campo " + error.getField() + " " + error.getDefaultMessage())
                .toList();
        response.put(ERROR, errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ReservaSolapadaException.class)
    public ResponseEntity<Map<String, Object>> handleHorarioInvalidoException(ReservaSolapadaException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ReservaModificadaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaModificadaException(ReservaModificadaException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Maneja errores de restricción de tipo de aula o permisos (ResponseStatusException)
     * Ejemplo: estudiante intenta reservar un tipo de aula no permitido → 400
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getReason());
        response.put(STATUS, ex.getStatusCode().value());
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Maneja errores de Feign cuando el aula-service no encuentra el aula
     * Ejemplo: codigoAula no existe en la base de datos del aula-service
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        HashMap<String, Object> response = new HashMap<>();
        if (ex.status() == 404) {
            response.put(MESSAGE, "El aula consultada no existe en el sistema.");
            response.put(STATUS, 404);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        response.put(MESSAGE, "Error al comunicarse con el servicio de aulas: " + ex.getMessage());
        response.put(STATUS, ex.status());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(ReservaNoPermitidaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaNoPermitidaException(ReservaNoPermitidaException ex){
        HashMap<String, Object> response = new HashMap<>();
        response.put(MESSAGE, ex.getMessage());
        response.put(STATUS, HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

}
