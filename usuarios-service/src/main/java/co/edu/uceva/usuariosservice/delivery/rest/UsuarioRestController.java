package co.edu.uceva.usuariosservice.delivery.rest;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.service.IUsuarioService;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuario-service")
public class UsuarioRestController {

    private final IUsuarioService usuarioService;

    private static final String ERROR = "error";
    private static final String ERRORS = "errors";
    private static final String MENSAJE = "mensaje";
    private static final String USUARIO = "usuario";
    private static final String USUARIOS = "usuarios";

    public UsuarioRestController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Map<String, Object>> getUsuarios() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Usuario> usuarios = usuarioService.findAll();

            if (usuarios.isEmpty()) {
                response.put(MENSAJE, "No hay Usuarios en la base de datos.");
                response.put(USUARIOS, usuarios); // para que sea siempre el mismo campo
                return ResponseEntity.status(HttpStatus.OK).body(response); // 200 pero lista vacía
            }

            response.put(USUARIOS, usuarios);
            return ResponseEntity.ok(response);
        }catch (DataAccessException e) {
            response.put(MENSAJE, "Error al consultar la base de datos");
            response.put(ERROR, e.getMessage().concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/usuarios/page/{page}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO')")
    public ResponseEntity<Object> index(@PathVariable Integer page) {
        Map<String, Object> response = new HashMap<>();
        Pageable pageable = PageRequest.of(page,4);

        try {
            Page<Usuario> usuarios = usuarioService.findAll(pageable);

            if (usuarios.isEmpty()) {
                response.put(MENSAJE, "No hay usuarios en la página solicitada.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(usuarios);

        } catch (DataAccessException e) {
            response.put(MENSAJE, "Error al consultar la base de datos.");
            response.put(ERROR, e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (IllegalArgumentException e) {
            response.put(MENSAJE, "Número de página inválido.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/usuarios/tipo/{id}")

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> save(@Valid @RequestBody Usuario usuario, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
                    .toList();

            response.put(ERRORS, errors);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Usuario nuevoUsuario = usuarioService.save(usuario);

            response.put(MENSAJE, "El usuario ha sido creado con éxito!");
            response.put(USUARIO, nuevoUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DataAccessException e) {
            response.put(MENSAJE, "Error al insertar el usuario en la base de datos.");
            response.put(ERROR, e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Usuario usuarioExistente = usuarioService.findById(id);
            if (usuarioExistente == null) {
                response.put(MENSAJE, "El usuario ID: " + id + " no existe en la base de datos.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            usuarioService.delete(usuarioExistente);
            response.put(MENSAJE, "El usuario ha sido eliminado con éxito!");
            return ResponseEntity.ok(response);
        } catch (DataAccessException e) {
            response.put(MENSAJE, "Error al eliminar el usuario de la base de datos.");
            response.put(ERROR, e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody Usuario usuario, BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(err -> "El campo '" + err.getField() + "' " + err.getDefaultMessage())
                    .toList();

            response.put("errors", errors);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            if (usuarioService.findById(id) == null) {
                response.put(MENSAJE, "Error: No se pudo editar, el usuario ID: " + id + " no existe en la base de datos.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            usuario.setCodigo(id); // Aseguramos usar el ID de la URL
            Usuario usuarioActualizado = usuarioService.save(usuario);

            response.put(MENSAJE, "El usuario ha sido actualizado con éxito!");
            response.put(USUARIO, usuarioActualizado);
            return ResponseEntity.ok(response);

        } catch (DataAccessException e) {
            response.put(MENSAJE, "Error al actualizar el usuario en la base de datos.");
            response.put(ERROR, e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Usuario usuario = usuarioService.findById(id);

            if (usuario == null) {
                response.put(MENSAJE, "El usuario ID: " + id + " no existe en la base de datos.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put(MENSAJE, "El usuario ha sido encontrado!");
            response.put(USUARIO, usuario);
            return ResponseEntity.ok(response);

        } catch (DataAccessException e) {
            response.put(MENSAJE, "Error al consultar la base de datos.");
            response.put(ERROR, e.getMessage().concat(": ").concat(e.getMostSpecificCause().getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
