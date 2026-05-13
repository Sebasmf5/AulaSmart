package co.edu.uceva.reservaservice.domain.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "aula-service", url = "${url.servicio.asistencia}", configuration = co.edu.uceva.reservaservice.config.FeignClientInterceptor.class)
public interface IAulaClient {
    @GetMapping("/api/v1/aula-service/aulas/tipo/{codigo}")
    String getTipoDeAula(@PathVariable("codigo") Long codigo);

    @GetMapping("/api/v1/aula-service/aulas/requiere-autorizacion/{codigo}")
    Boolean getRequiereAutorizacion(@PathVariable("codigo") Long codigo);

    @GetMapping("/api/v1/aula-service/aulas/siga/{codigo}")
    Integer getSigaDeAula(@PathVariable("codigo") Long codigo);

    /** Devuelve todos los codigosAula registrados en el aula-service. */
    @GetMapping("/api/v1/aula-service/aulas/codigos")
    List<Long> listarCodigosAula();

    /** Devuelve solo los codigosAula de aulas sincronizadas con SIGA. */
    @GetMapping("/api/v1/aula-service/aulas/codigos-siga")
    List<Long> listarCodigosAulaSiga();
}