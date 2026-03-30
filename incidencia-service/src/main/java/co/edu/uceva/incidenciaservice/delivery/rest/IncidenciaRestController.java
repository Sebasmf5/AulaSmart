package co.edu.uceva.incidenciaservice.delivery.rest;

import co.edu.uceva.incidenciaservice.domain.exception.IncidenciaNoEncontradaException;
import co.edu.uceva.incidenciaservice.domain.exception.NoHayIncidenciasException;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.service.IIncidenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> save(@RequestBody Incidencia incidencia) {
        // En este paso llamamos al servicio que guardará y a futuro generará la carta con IA
        Incidencia nuevaIncidencia = incidenciaService.save(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha reportado y guardado con éxito!");
        response.put(INCIDENCIA, nuevaIncidencia);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/incidencias")
    public ResponseEntity<Map<String, Object>> findAll() {
        List<Incidencia> incidencias = incidenciaService.findAll();
        if (incidencias.isEmpty()) {
            throw new NoHayIncidenciasException();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put(INCIDENCIAS, incidencias);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incidencias/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Incidencia incidencia = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
                
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "Incidencia encontrada con éxito!");
        response.put(INCIDENCIA, incidencia);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/incidencias/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Incidencia incidencia = incidenciaService.findById(id)
                .orElseThrow(() -> new IncidenciaNoEncontradaException(id));
                
        incidenciaService.delete(incidencia);
        
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "La incidencia se ha eliminado correctamente!");
        return ResponseEntity.ok(response);
    }
}
