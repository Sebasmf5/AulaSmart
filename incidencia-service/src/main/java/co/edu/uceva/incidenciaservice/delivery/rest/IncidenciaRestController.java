package co.edu.uceva.incidenciaservice.delivery.rest;

import co.edu.uceva.incidenciaservice.domain.exception.IncidenciaNoEncontradaException;
import co.edu.uceva.incidenciaservice.domain.exception.NoHayIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.PaginaSinIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.exception.ValidationException;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.service.IIncidenciaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

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
    public ResponseEntity<Map<String, Object>> save(@Valid @RequestBody Incidencia incidencia, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        Incidencia nuevaIncidencia = incidenciaService.save(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha reportado y guardado con éxito!");
        response.put(INCIDENCIA, nuevaIncidencia);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/incidencias/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody Incidencia incidencia, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        // Validamos que exista antes de actualizar
        Incidencia incidenciaExistente = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
        
        // Aseguramos que el ID de la ruta siga siendo el mismo
        incidencia.setId(incidenciaExistente.getId());
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
}
