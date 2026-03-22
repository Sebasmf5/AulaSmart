package co.edu.uceva.reservaservice.delevery.rest;

import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.service.IReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaRestController {

    private static final String RESERVAS = "reservas";
    private static final String RESERVA = "reserva";
    private static final String MENSAJE = "mensaje";

    private final IReservaService reservaService;

    /**
     * Listar todas las reservas.
     */
    @GetMapping("/reservas")
    public ResponseEntity<Map<String, Object>> getReservas() {
        List<Reserva> reservas = reservaService.findAll();
        if (reservas.isEmpty()) {
            System.out.println("No se encontraron reservas");
        }
        Map<String, Object> response = new HashMap<>();
        response.put(RESERVAS, reservas);
        return ResponseEntity.ok(response);
    }

    /**
     * Listar reservas con paginación.
     */
    @GetMapping("/reserva/page/{page}")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Reserva> reservas = reservaService.findAll(pageable);
        if (reservas.isEmpty()) {
            System.out.printf("No se encontraron reservas en la pagina: " + page);
        }
        return ResponseEntity.ok(reservas);
    }

    /**
     * Reservar pasando el objeto en el cuerpo de la petición, usando validaciones
     */
    @PostMapping("/reservas")
    public ResponseEntity<Map<String, Object>> addReserva(@Valid @RequestBody Reserva reserva, BindingResult result) {
        if (result.hasErrors()) {
            System.out.printf("Error en el cuerpo de solicitud: %s", result.getAllErrors());
        }
        Map<String, Object> response = new HashMap<>();
        Reserva nuevaReserva = reservaService.addReserva(reserva);
        response.put(MENSAJE, "La reserva ha sido creada con éxito!");
        response.put(RESERVA, nuevaReserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Eliminar una reserva pasando el objeto en el cuerpo de la petición.
     */
    @DeleteMapping("/reservas")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody Reserva reserva) {
        reservaService.findReservaById(reserva.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe"));
        reservaService.deleteReserva(reserva);
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "la reserva ha sido eliminado con éxito!");
        response.put(RESERVA, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener una reserva por su ID.
     */
    @GetMapping("/reservas/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Reserva reserva = reservaService.findReservaById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe"));
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "El producto ha sido encontrado con éxito!");
        response.put(RESERVA, reserva);
        return ResponseEntity.ok(response);
    }

}