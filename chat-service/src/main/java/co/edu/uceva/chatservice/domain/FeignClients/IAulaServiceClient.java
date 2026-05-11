package co.edu.uceva.chatservice.domain.FeignClients;

import co.edu.uceva.chatservice.domain.config.FeignClientInterceptor;
import co.edu.uceva.chatservice.domain.dto.BloqueDTO;
import co.edu.uceva.chatservice.domain.dto.ResponseAulaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(
        name = "aula-service",
        url = "${conexion.servicios.aula-service.url}",
        configuration = FeignClientInterceptor.class
)
public interface IAulaServiceClient {
    @GetMapping("/api/v1/aula-service/aulas")
    Map<String, Object> listarAulas();

    @GetMapping("/api/v1/aula-service/aulas/bloque/{bloqueId}")
    ResponseAulaDTO listarAulasPorBloque(@PathVariable Long bloqueId);

    @GetMapping("/api/v1/aula-service/aulas/facultad/{facultadId}")
    ResponseAulaDTO listarAulasPorFacultad(@PathVariable Long facultadId);

    @GetMapping("/api/v1/aula-service/bloques")
    Map<String, Object> listarBloques();

    @GetMapping("/api/v1/aula-service/facultades")
    Map<String, Object> listarFacultades();

    @GetMapping("/api/v1/aula-service/bloques/facultad/{facultadId}")
    Map<String, Object> listarBloquesPorFacultad(@PathVariable Long facultadId);

    @GetMapping("/api/v1/aula-service/aulas/buscar/{nombre}")
    ResponseAulaDTO buscarAulasPorNombre(@PathVariable("nombre") String nombre);

    @GetMapping("/api/v1/aula-service/aulas/tipo-aula/{tipoAula}")
    ResponseAulaDTO listarAulasPorTipoAula(@PathVariable String tipoAula);

    @GetMapping("/api/v1/aula-service/bloques/buscar/{nombre}")
    Map<String, BloqueDTO> buscarBloque(@PathVariable("nombre") String nombre);

    @GetMapping("/api/v1/aula-service/tipos-aula")
    Map<String, Object> listarTiposAula();
}
