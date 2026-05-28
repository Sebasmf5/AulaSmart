package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.service.ITipoAulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aula-service/tipos-aula")
@RequiredArgsConstructor
public class TipoAulaRestController {

    private final ITipoAulaService tipoAulaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarTodos() {
        Map<String, Object> response = new HashMap<>();
        response.put("tiposAula", tipoAulaService.listarTodos());
        return ResponseEntity.ok(response);
    }
}
