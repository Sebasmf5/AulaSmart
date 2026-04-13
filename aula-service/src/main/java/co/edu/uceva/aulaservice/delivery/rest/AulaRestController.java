package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.exception.*;
import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.service.IAulaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;


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
     * Crear una nueva aula pasando el objeto en el cuerpo de la petición, usando validaciones
     */
    @PostMapping("/aulas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> save(@Valid @RequestBody Aula aula, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        Map<String, Object> response = new HashMap<>();
        Aula nuevoAula = aulaService.save(aula);
        response.put(MENSAJE, "El aula ha sido creado con éxito!");
        response.put(AULA, nuevoAula);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Eliminar un aula pasando el objeto en el cuerpo de la petición.
     */
    @DeleteMapping("/aulas/{id}") // El ID va en la URL
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        // Verificamos existencia antes de borrar
        aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));

        aulaService.deleteById(id); // Usamos el ID directamente

        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "El aula ha sido eliminada con éxito!");
        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar un aula pasando el objeto en el cuerpo de la petición.
     * @param aula: Objeto Aula que se va a actualizar
     */
    @PutMapping("/aulas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Aula aula, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        Long id = aula.getId();
        aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));
        Map<String, Object> response = new HashMap<>();
        Aula aulaActualizado = aulaService.update(aula);
        response.put(MENSAJE, "El aula ha sido actualizado con éxito!");
        response.put(AULA, aulaActualizado);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener un aula por su ID.
     */
    @GetMapping("/aulas/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Aula aula = aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "El aula ha sido encontrado con éxito!");
        response.put(AULA, aula);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener un aula por su ID.
     */
    @GetMapping("/aulas/codigo/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findByCodigo(@PathVariable Long codigo) {
        Aula aula = aulaService.obtenerAula(codigo)
                .orElseThrow(() -> new AulaCodigoNoEncontrada(codigo));
        Map<String, Object> response = new HashMap<>();
        response.put(MENSAJE, "El aula ha sido encontrado con éxito!");
        response.put(AULA, aula);
        return ResponseEntity.ok(response);
    }

    /*
    * Obtener el tipo de aula por el códigoDelAula, necesario para la reserva de los estudiantes
    * */
    @GetMapping("/aulas/tipo/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<String> getTipoDeAula(@PathVariable Long codigo) {
        String tipoDeAula = aulaService.obtenerTipoAula(codigo);
        if (tipoDeAula == null) {
            throw new AulaNoEncontradaException(codigo);
        }
        // Devolvemos el String directamente.
        return ResponseEntity.ok(tipoDeAula);
    }

    /*
    * Obtener si el aula debe pasar por el administrador para aprobar el aula
    * */
    @GetMapping("aulas/requiere-autorizacion/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Boolean> getRequiereAutorizacion(@PathVariable Long codigo) {
        Aula aula = aulaService.obtenerAula(codigo)
                .orElseThrow(() -> new AulaCodigoNoEncontrada(codigo));
        Boolean isAutorizable = aula.getRequiereAutorizacion();
        // devolver el valor (true o false)
        return ResponseEntity.ok(isAutorizable);
    }


    /**
     * Listar todas las aulas.
     */
    @GetMapping("/aulas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Pageable pageable = PageRequest.of(page, 4);
        Page<Aula> aulas = aulaService.findAll(pageable);
        if (aulas.isEmpty()) {
            throw new PaginaSinAulasException(page);
        }
        return ResponseEntity.ok(aulas);
    }

}
