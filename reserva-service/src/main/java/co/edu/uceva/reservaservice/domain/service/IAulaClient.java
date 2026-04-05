package co.edu.uceva.reservaservice.domain.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "aula-service", url = "${url.servicio.asistencia}")
public interface IAulaClient {
    @GetMapping("/api/v1/aula-service/aulas/tipo/{codigo}")
    String getTipoDeAula(@PathVariable("codigo") Long codigo);
}