package co.edu.uceva.reservaservice.delevery.rest;

import co.edu.uceva.reservaservice.domain.excepcion.NoHayReservasException;
import co.edu.uceva.reservaservice.domain.excepcion.PaginaSinReservasException;
import co.edu.uceva.reservaservice.domain.excepcion.ReservaNoEncontradaException;
import co.edu.uceva.reservaservice.domain.excepcion.ValidationException;
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
@RequestMapping("/api/v1/reserva-service")
public class ReservaRestController {

    private static final String RESERVAS = "reservas";
    private static final String RESERVA = "reserva";
    private static final String MENSAJE = "mensaje";

    private final IReservaService reservaService;

    // Inyección de dependencia del servicio que proporciona servicios de CRUD
    public ReservaRestController(IReservaService reservaService) {
        this.reservaService = reservaService;
    }
    /**
     * Listar todas las reservas.
     */
    @GetMapping("/reservas")
    public ResponseEntity<Map<String, Object>> getReservas() {
        List<Reserva> reservas = reservaService.findAll();
        if (reservas.isEmpty()) {
            throw new NoHayReservasException();
        }
        Map<String, Object> response = new HashMap<>();
        response.put(RESERVAS, reservas);
        return ResponseEntity.ok(response);
    }

    /**
     * Listar reservas con paginación.
     */
    @GetMapping("/reservas/page/{page}")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Reserva> reservas = reservaService.findAll(pageable);
        if (reservas.isEmpty()) {
            throw new PaginaSinReservasException(page);
        }
        return ResponseEntity.ok(reservas);
    }

    /**
     * Reservar pasando el objeto en el cuerpo de la petición, usando validaciones
     */
    @PostMapping("/reservas")
    public ResponseEntity<Map<String, Object>> addReserva(@Valid @RequestBody Reserva reserva, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
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
                .orElseThrow(() -> new ReservaNoEncontradaException(reserva.getId()));
        reservaService.deleteReserva(reserva);
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "la reserva ha sido eliminado con éxito!");
        response.put(RESERVA, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar una reserva pasando el objeto en el cuerpo de la petición.
     * @param reserva: Objeto Reserva que se va a actualizar
     */
    @PutMapping("/reservas")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Reserva reserva, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        reservaService.findReservaById(reserva.getId())
                .orElseThrow(() -> new ReservaNoEncontradaException(reserva.getId()));
        Map<String, Object> response = new HashMap<>();
        Reserva reservaActualizado = reservaService.updateReserva(reserva);
        response.put(MENSAJE, "La reserva ha sido actualizado con éxito!");
        response.put(RESERVA, reservaActualizado);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener una reserva por su ID.
     */
    @GetMapping("/reservas/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Reserva reserva = reservaService.findReservaById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "El producto ha sido encontrado con éxito!");
        response.put(RESERVA, reserva);
        return ResponseEntity.ok(response);
    }



}