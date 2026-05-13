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
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;

import javax.swing.text.html.Option;


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
    public ResponseEntity<Map<String, Object>>  save(@Valid @RequestBody Aula aula, BindingResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        // Las aulas creadas manualmente por defecto no están en SIGA
        if (aula.getSincronizadaConSiga() == null) {
            aula.setSincronizadaConSiga(false);
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

    @GetMapping("/aulas/siga/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Long> getPasaporteSiga(@PathVariable Long codigo) {
        Aula aula = aulaService.obtenerAula(codigo)
                .orElseThrow(() -> new AulaCodigoNoEncontrada(codigo));
        // Devolvemos el codigoAula (código SIGA) directamente.
        return ResponseEntity.ok(aula.getCodigoAula());
    }

    /*
    * Obtener si el aula debe pasar por el administrador para aprobar el aula
    * */
    @GetMapping("aulas/requiere-autorizacion/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Boolean> getRequiereAutorizacion(@PathVariable Long codigo) {
        Aula aula = aulaService.obtenerAula(codigo)
                .orElseThrow(() -> new AulaCodigoNoEncontrada(codigo));
        Boolean isAutorizable = aula.getTipoAula().getRequiereAutorizacion();
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

    @GetMapping("/aulas/bloque/{bloqueId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarAulasPorBloque(@PathVariable Long bloqueId) {
        List<Aula> aulas = aulaService.filtrarPorBloque(bloqueId);
        Map<String, Object> response = new HashMap<>();
        response.put(AULAS, aulas);
        response.put(MENSAJE, "Aulas obtenidas correctamente para el bloque " + bloqueId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/aulas/facultad/{facultadId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarAulasPorFacultad(@PathVariable Long facultadId) {
        List<Aula> aulas = aulaService.filtrarPorFacultad(facultadId);
        Map<String, Object> response = new HashMap<>();
        response.put(AULAS, aulas);
        response.put(MENSAJE, "Aulas obtenidas correctamente para la facultad " + facultadId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/aulas/buscar/{nombreAula}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarAulasPorNombre(@PathVariable String nombreAula) {
        List<Aula> aulas = aulaService.filtrarPorNombre(nombreAula);
        Map<String, Object> response = new HashMap<>();
        response.put(AULAS, aulas);
        response.put(MENSAJE, "Aulas obtenidas correctamente para la búsqueda: " + nombreAula);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/aulas/tipo-aula/{tipoAula}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> listarAulasPorTipoAula(@PathVariable String tipoAula) {
        List<Aula> aulas = aulaService.filtrarPorTipoAula(tipoAula);
        Map<String, Object> response = new HashMap<>();
        response.put(AULAS, aulas);
        response.put(MENSAJE, "Aulas obtenidas correctamente para el tipo: " + tipoAula);
        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve todos los codigosAula registrados en el sistema.
     */
    @GetMapping("/aulas/codigos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<List<Long>> listarCodigosAula() {
        List<Long> codigos = aulaService.findAll()
                .stream()
                .map(Aula::getCodigoAula)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(codigos);
    }

    /**
     * Devuelve los codigosAula de aulas que están sincronizadas con SIGA.
     * Solo estas aulas deben consultarse contra el sistema SIGA para verificar disponibilidad.
     */
    @GetMapping("/aulas/codigos-siga")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<List<Long>> listarCodigosAulaSiga() {
        List<Long> codigos = aulaService.findAll()
                .stream()
                .filter(Aula::getSincronizadaConSiga)
                .map(Aula::getCodigoAula)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(codigos);
    }

    // ── Endpoints por ID de base de datos (PK) para uso interno entre microservicios ──

    @GetMapping("/aulas/{id}/tipo")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<String> getTipoDeAulaById(@PathVariable Long id) {
        Aula aula = aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));
        return ResponseEntity.ok(aula.getTipoAula().getCodigoTipoAula());
    }

    @GetMapping("/aulas/{id}/requiere-autorizacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Boolean> getRequiereAutorizacionById(@PathVariable Long id) {
        Aula aula = aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));
        return ResponseEntity.ok(aula.getTipoAula().getRequiereAutorizacion());
    }

    @GetMapping("/aulas/{id}/codigo-siga")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Long> getCodigoSigaById(@PathVariable Long id) {
        Aula aula = aulaService.findById(id)
                .orElseThrow(() -> new AulaNoEncontradaException(id));
        return ResponseEntity.ok(aula.getCodigoAula());
    }

    /**
     * Devuelve las aulas sincronizadas con SIGA como lista de objetos {id, codigoAula}.
     * Usado por reserva-service para mapear aulaId (PK) a codigoAula (pasaporte SIGA).
     */
    @GetMapping("/aulas/sincronizadas-siga")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<List<Map<String, Object>>> listarAulasSincronizadasSiga() {
        List<Aula> aulas = aulaService.findAll()
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getSincronizadaConSiga()))
                .toList();
        List<Map<String, Object>> resultado = aulas.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", a.getId());
                    map.put("codigoAula", a.getCodigoAula());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(resultado);
    }
}
