package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.model.Bloque;
import co.edu.uceva.aulaservice.domain.service.IBloqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aula-service/bloques")
@RequiredArgsConstructor
public class BloqueRestController {

    private final IBloqueService bloqueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarTodos() {
        Map<String, Object> response = new HashMap<>();
        response.put("bloques", bloqueService.listarTodos());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/facultad/{facultadId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarPorFacultad(@PathVariable Long facultadId) {
        Map<String, Object> response = new HashMap<>();
        response.put("bloques", bloqueService.listarPorFacultad(facultadId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/buscar/{nombre}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> filtrarPorNombre(@PathVariable String nombre) {
        Map<String, Object> response = new HashMap<>();
        response.put("bloque", bloqueService.filtrarPorNombre(nombre));
        return ResponseEntity.ok(response);
    }
}
