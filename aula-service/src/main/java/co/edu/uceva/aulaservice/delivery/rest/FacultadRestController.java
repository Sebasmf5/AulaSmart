package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.model.Facultad;
import co.edu.uceva.aulaservice.domain.service.IFacultadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aula-service/facultades")
@RequiredArgsConstructor
public class FacultadRestController {

    private final IFacultadService facultadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarTodas() {
        Map<String, Object> response = new HashMap<>();
        response.put("facultades", facultadService.listarTodas());
        return ResponseEntity.ok(response);
    }
}
