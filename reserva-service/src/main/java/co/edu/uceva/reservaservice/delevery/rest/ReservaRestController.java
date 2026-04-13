package co.edu.uceva.reservaservice.delevery.rest;

import co.edu.uceva.reservaservice.domain.excepcion.*;
import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.service.IAulaClient;
import co.edu.uceva.reservaservice.domain.service.IReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final IAulaClient iaulaClient;

    // Inyección de dependencia del servicio que proporciona servicios de CRUD
    public ReservaRestController(IReservaService reservaService, IAulaClient iaulaClient) {
        this.reservaService = reservaService;
        this.iaulaClient = iaulaClient;
    }
    /**
     * Listar todas las reservas.
     */
    @GetMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Reserva> reservas = reservaService.findAll(pageable);
        if (reservas.isEmpty()) {
            throw new PaginaSinReservasException(page);
        }
        return ResponseEntity.ok(reservas);
    }

    // Actualmente SOLO para estudiantes
    /**
     * Reservar pasando el objeto en el cuerpo de la petición, usando validaciones:
     *  ESTUDIANTE SOLO puede reservar unos tipos de aula asignados previamente
     *  DOCENTE puede reservar sin restricción
     *  Hay unas aulas que requieren previa autorización, por lo que su estado hasta su aprobación sería de PENDIENTE
     */
    @PostMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> addReserva(@Valid @RequestBody Reserva reserva, BindingResult result) {
        Map<String, Object> response = new HashMap<>();
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        validarPermisosDeAula(reserva.getCodigoAula());
        Reserva nuevaReserva = reservaService.addReserva(reserva);
        if (iaulaClient.getRequiereAutorizacion(nuevaReserva.getCodigoAula())) {
            nuevaReserva.setEstado(EstadosReserva.PENDIENTE);
        } else {
            nuevaReserva.setEstado(EstadosReserva.CONFIRMADA);
        }
        response.put(MENSAJE, "La reserva ha sido creada con éxito!");
        response.put(RESERVA, nuevaReserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/reservas/aprobar/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> updateReserva(@PathVariable Long id) {
        Reserva reservaExistente = reservaService.findReservaById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));

        reservaExistente.setEstado(EstadosReserva.CONFIRMADA);
        Reserva reservaActualizado = reservaService.updateReserva(reservaExistente);

        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La reserva ha sido aprobada con éxito!");
        response.put(RESERVA, reservaActualizado);
        return ResponseEntity.ok(response);
    }


    /**
     * Eliminar una reserva pasando el objeto en el cuerpo de la petición.
     * Tener en cuenta que un DOCENTE o ESTUDIANTE eliminar su propia reserva
     * Solo el ADMINISTRATIVO puede eliminar esa reserva
     */
    @DeleteMapping("/reservas/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Reserva reservaExistente = reservaService.findReservaById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String rol = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority() : "";
        Long userId = Long.parseLong(authentication.getName());

        if (!"ROLE_ADMINISTRADOR".equals(rol) && !"ROLE_ADMINISTRATIVO".equals(rol) && !reservaExistente.getIdSolicitante().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes eliminar una reserva que no te pertenece.");
        }

        reservaService.deleteReserva(reservaExistente);
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La reserva ha sido eliminada con éxito!");
        response.put(RESERVA, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar una reserva pasando el objeto en el cuerpo de la petición.
     * @param reserva: Objeto Reserva que se va a actualizar
     */

    @PutMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
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
        Reserva reservaExistente = reservaService.findReservaById(reserva.getIdReserva())
                .orElseThrow(() -> new ReservaNoEncontradaException(reserva.getIdReserva()));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String rol = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority() : "";
        Long userId = Long.parseLong(authentication.getName());

        if (!"ROLE_ADMINISTRADOR".equals(rol) && !"ROLE_ADMINISTRATIVO".equals(rol) && !reservaExistente.getIdSolicitante().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes actualizar una reserva que no te pertenece.");
        }

        validarPermisosDeAula(reserva.getCodigoAula());

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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Reserva reserva = reservaService.findReservaById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La reserva ha sido encontrado con éxito!");
        response.put(RESERVA, reserva);
        return ResponseEntity.ok(response);
    }

    // metodo para extraer la información necesaria de los permisos
    private void validarPermisosDeAula(Long codigo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String rol = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority() : "";
        String tipoAula = iaulaClient.getTipoDeAula(codigo);
        int tipo = Integer.parseInt(tipoAula);
        System.out.println(tipo);
        if ("ROLE_ESTUDIANTE".equals(rol)) {
            if (tipo != 1 && tipo != 5 && tipo != 78) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El tipo de aula recibido no es válido para reservas de Estudiante.");
            }
        } else if ("ROLE_ADMINISTRADOR".equals(rol) || "ROLE_DOCENTE".equals(rol) || "ROLE_ADMINISTRATIVO".equals(rol)) {
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol no autorizado para reservar.");
        }
    }
}