package co.edu.uceva.reservaservice.delevery.excepcion;

import co.edu.uceva.reservaservice.domain.excepcion.*;
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


}
