package co.edu.uceva.aulaservice.delivery.rest;

import co.edu.uceva.aulaservice.domain.service.IAulaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
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

        //declaramos final el servicio para que no se pueda modificar y lo inyectamos por el constructor
        private final IAulaService aulaService;

    private static final String MENSAJE = "mensaje";

        public AulaRestController(IAulaService aulaService) {
            this.aulaService = aulaService;
        }

    /**
     * 🔹 Listar todas las aulas
     */
    @GetMapping("/aulas")
    public ResponseEntity<List<Aula>> obtenerTodas() {
        List<Aula> aulas = aulaService.findAll();
        if (aulas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(aulas);
    }

    /**
     * 🔹 Listar aulas por paginación
     */
    @GetMapping("/aula/page/{page}")
    public ResponseEntity<List<Aula>> obtenerPaginado(@PathVariable Integer page) {
        List<Aula> aulas = aulaService.findAll(PageRequest.of(page, 4)).getContent();
        if (aulas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(aulas);
    }
    /**
     * 🔹 Crear aula (carga inicial desde JSON o manual)
     */
    @PostMapping("aula" )
    public Aula crear(@RequestBody Aula aula) {
        return aulaService.save(aula);

    }

    //pruebas
    @PostMapping("/aulas")
    public List<Aula> crearMasivo(@RequestBody List<Aula> aulas) {
        return aulas.stream()
                .map(aulaService::save)
                .toList();
    }

    /**
     * 🔹 Eliminar aula
     */
    @DeleteMapping("aula/{id}")
    public void eliminar(@PathVariable Long id) {
        aulaService.delete(aulaService.findById(id).orElseThrow(() -> new RuntimeException("Aula no encontrada con id: " + id)));
    }

    /**
     * 🔹 Actualizar aula
     */
    @PutMapping("/aulas")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Aula aula, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        aulaService.findById(aula.getId());
        Map<String, Object> response = new HashMap<>();
        Aula aulaActualizada = aulaService.update(aula);
        response.put(MENSAJE, "El aula ha sido actualizada con éxito!");
        response.put("aula", aulaActualizada);
        return ResponseEntity.ok(response);
    }


    /**
     * 🔹 Obtener aula por ID
     */
    @GetMapping("aula/{id}")
    public Aula obtenerPorId(@PathVariable Long id) {
        return aulaService.findById(id).orElseThrow(() -> new RuntimeException("Aula no encontrada con id: " + id));
        }

}
