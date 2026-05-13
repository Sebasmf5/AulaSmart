package co.edu.uceva.reservaservice.domain.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "aula-service", url = "${url.servicio.asistencia}", configuration = co.edu.uceva.reservaservice.config.FeignClientInterceptor.class)
public interface IAulaClient {

    // ── Consultas por ID de base de datos (PK) ─────────────────────────────

    @GetMapping("/api/v1/aula-service/aulas/{id}/tipo")
    String getTipoDeAula(@PathVariable("id") Long aulaId);

    @GetMapping("/api/v1/aula-service/aulas/{id}/requiere-autorizacion")
    Boolean getRequiereAutorizacion(@PathVariable("id") Long aulaId);

    /**
     * Obtiene el codigoAula (pasaporte SIGA) de un aula dado su ID interno.
     * Usado para consultar el sistema SIGA externo.
     */
    @GetMapping("/api/v1/aula-service/aulas/{id}/codigo-siga")
    Long getCodigoSigaDeAula(@PathVariable("id") Long aulaId);

    /**
     * Devuelve las aulas sincronizadas con SIGA como lista de {id, codigoAula}.
     */
    @GetMapping("/api/v1/aula-service/aulas/sincronizadas-siga")
    List<Map<String, Object>> listarAulasSincronizadasSiga();

    // ── Endpoints legacy (por codigoAula) - mantenidos para compatibilidad ──

    @GetMapping("/api/v1/aula-service/aulas/codigos")
    List<Long> listarCodigosAula();
}
