package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulaservice.domain.exception.NoHayAulasException;
import co.edu.uceva.aulaservice.domain.exception.PaginaSinAulasException;
import co.edu.uceva.aulaservice.domain.service.IAulaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.exception.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/aula-service")
public class AulaRestController {

    // Declaramos como final el servicio para mejorar la inmutabilidad
    private final IAulaService aulaService;

    // Constantes para los mensajes de respuesta
    private static final String MENSAJE = "mensaje";
    private static final String AULA = "aula";
    private static final String AULAS = "aulas";

    // Inyección de dependencia del servicio que proporciona servicios de CRUD
    public AulaRestController(IAulaService aulaService) {
        this.aulaService = aulaService;
    }

    /**
     * Listar todos los aulas.
     */
    @GetMapping("/aulas")
    public ResponseEntity<Map<String, Object>> getAulas() {
        List<Aula> aulas = aulaService.findAll();
        if (aulas.isEmpty()) {
            throw new NoHayAulasException();
        }
        Map<String, Object> response = new HashMap<>();
        response.put(AULAS, aulas);
        return ResponseEntity.ok(response);
    }

    /**
     * Listar aulas con paginación.
     */
    @GetMapping("/aula/page/{page}")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Aula> aulas = aulaService.findAll(pageable);
        if (aulas.isEmpty()) {
            throw new PaginaSinAulasException(page);
        }
        return ResponseEntity.ok(aulas);
    }

}
