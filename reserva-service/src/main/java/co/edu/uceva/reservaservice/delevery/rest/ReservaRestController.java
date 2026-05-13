package co.edu.uceva.reservaservice.delevery.rest;

import co.edu.uceva.reservaservice.domain.excepcion.NoHayReservasException;
import co.edu.uceva.reservaservice.domain.excepcion.PaginaSinReservasException;
import co.edu.uceva.reservaservice.domain.excepcion.ReservaNoEncontradaException;
import co.edu.uceva.reservaservice.domain.excepcion.ValidationException;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.service.IAulaClient;
import co.edu.uceva.reservaservice.domain.service.IReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.LockMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import co.edu.uceva.reservaservice.domain.integration.service.AgregadorReservasService;
import co.edu.uceva.reservaservice.domain.integration.dto.ReservaDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import co.edu.uceva.reservaservice.domain.model.RolUsuario;

@RestController
@RequestMapping("/api/v1/reserva-service")
public class ReservaRestController {

    private static final String RESERVAS = "reservas";
    private static final String RESERVA = "reserva";
    private static final String MENSAJE = "mensaje";

    private final IReservaService reservaService;
    private final AgregadorReservasService agregadorReservasService;
    private final IAulaClient iaulaClient;

    // Inyección de dependencia del servicio que proporciona servicios de CRUD
    public ReservaRestController(IReservaService reservaService, AgregadorReservasService agregadorReservasService, IAulaClient iaulaClient) {
        this.reservaService = reservaService;
        this.iaulaClient = iaulaClient;
        this.agregadorReservasService = agregadorReservasService;
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
     * Listar reservas unificadas (AulaSmart + SIGA) por Aula (usando aulaId / PK).
     */
    @GetMapping("/reservas/aula/{aulaId}/agregadas")
    public ResponseEntity<Map<String, Object>> getReservasAgregadas(@PathVariable Long aulaId) {
        List<ReservaDTO> reservas = agregadorReservasService.obtenerTodasLasReservas(aulaId);
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
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            String codigoSolicitante = authentication.getPrincipal().toString();
            reserva.setIdSolicitante(Long.valueOf(codigoSolicitante));
            String nombreUsuario = authentication.getName();
            reserva.setNombreUsuarioResponsable(nombreUsuario);

            if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                String authority = authentication.getAuthorities().iterator().next().getAuthority(); // Ej: ROLE_ESTUDIANTE
                String rolStr = authority.replace("ROLE_", "");
                try {
                    reserva.setRolSolicitante(RolUsuario.valueOf(rolStr));
                } catch (IllegalArgumentException e) {
                    // Si el rol no mapea a RolUsuario
                }
            }
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
        reservaService.findReservaById(reserva.getIdReserva())
                .orElseThrow(() -> new ReservaNoEncontradaException(reserva.getIdReserva()));
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
        if (reserva.getVersion() == null ){
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(MENSAJE, "El campo 'version' es obligatorio para actualizar una reserva. " +
                    "Obtén la reserva primero con GET y usa el valor de version que recibes.");
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        reservaService.findReservaById(reserva.getIdReserva())
                .orElseThrow(() -> new ReservaNoEncontradaException(reserva.getIdReserva()));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            String codigoSolicitante = authentication.getPrincipal().toString();
            reserva.setIdSolicitante(Long.valueOf(codigoSolicitante));

            if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                String authority = authentication.getAuthorities().iterator().next().getAuthority(); // Ej: ROLE_ESTUDIANTE
                String rolStr = authority.replace("ROLE_", "");
                try {
                    reserva.setRolSolicitante(RolUsuario.valueOf(rolStr));
                } catch (IllegalArgumentException e) {
                    // Si el rol no mapea a RolUsuario
                }
            }
        }
        
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
        response.put(MENSAJE, "La reserva ha sido encontrado con éxito!");
        response.put(RESERVA, reserva);
        return ResponseEntity.ok(response);
    }

    public void restriccionReservas(){

    }

    /**
     * Obtener lista de IDs de aulas ocupadas en un rango de fechas/horas.
     * Combina reservas internas (AulaSmart BD) + reservas externas (SIGA universitario).
     * Formato esperado: fecha=yyyy-MM-dd, horaInicio=HH:mm, horaFin=HH:mm
     */
    @GetMapping("/reservas/ocupadas")
    public ResponseEntity<List<Long>> obtenerAulasOcupadas(
            @RequestParam String fecha,
            @RequestParam String horaInicio,
            @RequestParam String horaFin) {

        LocalDateTime inicio = LocalDateTime.parse(fecha + "T" + horaInicio + ":00");
        LocalDateTime fin    = LocalDateTime.parse(fecha + "T" + horaFin    + ":00");

        // BD interna + SIGA con detección de solapamiento
        List<Long> aulasOcupadas = agregadorReservasService.obtenerAulasOcupadasEnRangoConSiga(inicio, fin);
        return ResponseEntity.ok(aulasOcupadas);
    }
}