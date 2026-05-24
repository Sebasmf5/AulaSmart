package co.edu.uceva.incidenciaservice.delivery.rest;

import co.edu.uceva.incidenciaservice.domain.exception.IncidenciaNoEncontradaException;
import co.edu.uceva.incidenciaservice.domain.exception.NoHayIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.PaginaSinIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.ValidationException;
import co.edu.uceva.incidenciaservice.domain.model.EstadoIncidencia;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.service.IIncidenciaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/incidencia-service")
public class IncidenciaRestController {

    private final IIncidenciaService incidenciaService;

    // Constantes
    private static final String MENSAJE = "mensaje";
    private static final String INCIDENCIA = "incidencia";
    private static final String INCIDENCIAS = "incidencias";

    public IncidenciaRestController(IIncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    @PostMapping("/incidencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> save(@Valid @RequestBody Incidencia incidencia, BindingResult result, Authentication authentication) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        
        // Extraer el codigo_usuario desde el token JWT
        String codigoStr = (String) authentication.getPrincipal();
        incidencia.setCodigoUsuario(Long.valueOf(codigoStr));
        Incidencia nuevaIncidencia = incidenciaService.save(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha reportado y guardado con éxito!");
        response.put(INCIDENCIA, nuevaIncidencia);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Subir imagen de evidencia a una incidencia existente.
     * El usuario que creó la incidencia puede subir la imagen.
     */
    @PostMapping("/incidencias/{id}/imagen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> subirImagen(
            @PathVariable Long id,
            @RequestParam("imagen") MultipartFile imagen) {
        
        Incidencia incidencia = incidenciaService.guardarImagenEvidencia(id, imagen);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "Imagen de evidencia subida exitosamente.");
        response.put(INCIDENCIA, incidencia);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/incidencias/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody Incidencia incidencia, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        Incidencia incidenciaExistente = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
        
        incidencia.setId(incidenciaExistente.getId());
        incidencia.setCodigoUsuario(incidenciaExistente.getCodigoUsuario());
        incidencia.setFechaReporte(incidenciaExistente.getFechaReporte());
        
        Incidencia incidenciaActualizada = incidenciaService.update(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha actualizado con éxito!");
        response.put(INCIDENCIA, incidenciaActualizada);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incidencias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findAll() {
        List<Incidencia> incidencias = incidenciaService.findAll();
        if (incidencias.isEmpty()) {
            throw new NoHayIncidenciasException();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put(INCIDENCIAS, incidencias);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incidencias/page/{page}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Page<Incidencia>> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Incidencia> incidencias = incidenciaService.findAll(pageable);
        if (incidencias.isEmpty()) {
            throw new PaginaSinIncidenciasException();
        }
        return ResponseEntity.ok(incidencias);
    }

    @GetMapping("/incidencias/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Incidencia incidencia = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
                
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "Incidencia encontrada con éxito!");
        response.put(INCIDENCIA, incidencia);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/incidencias/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Incidencia incidencia = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
                
        incidenciaService.delete(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha eliminado correctamente!");
        return ResponseEntity.ok(response);
    }

    // ── NUEVOS ENDPOINTS ADMINISTRATIVOS ───────────────────────────────────

    /**
     * Contar incidencias pendientes (para badge/notificación).
     */
    @GetMapping("/incidencias/pendientes/count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> getCountPendientes() {
        long cantidad = incidenciaService.countByEstado(EstadoIncidencia.PENDIENTE);
        Map<String, Object> response = new HashMap<>();
        response.put("cantidad", cantidad);
        return ResponseEntity.ok(response);
    }

    /**
     * Listar incidencias pendientes de respuesta (solo administradores).
     */
    @GetMapping("/incidencias/pendientes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> getIncidenciasPendientes() {
        List<Incidencia> incidencias = incidenciaService.findByEstado(EstadoIncidencia.PENDIENTE);
        Map<String, Object> response = new HashMap<>();
        response.put(INCIDENCIAS, incidencias);
        return ResponseEntity.ok(response);
    }

    /**
     * Responder una incidencia (solo administradores).
     * Cambia el estado de PENDIENTE a REVISADA.
     */
    @PutMapping("/incidencias/{id}/responder")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> responderIncidencia(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        
        String respuesta = body.get("respuesta");
        if (respuesta == null || respuesta.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put(MENSAJE, "El campo 'respuesta' es obligatorio.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String codigoAdminStr = (String) auth.getPrincipal();
        Long codigoAdministrador = Long.valueOf(codigoAdminStr);

        Incidencia incidencia = incidenciaService.responderIncidencia(id, respuesta, codigoAdministrador);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia ha sido respondida exitosamente.");
        response.put(INCIDENCIA, incidencia);
        return ResponseEntity.ok(response);
    }
}
