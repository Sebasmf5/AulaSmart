package co.edu.uceva.aulaservice.domain.integration.scheduler;

import co.edu.uceva.aulaservice.domain.integration.SigaDTO.SigaResponseDTO;
import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.repository.IAulaRepository;
import co.edu.uceva.aulaservice.domain.integration.SigaDTO.AulaExternaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionAulasScheduler {

    private final RestClient restClient;
    private final IAulaRepository aulaRepository;

    @Value("${siga.api.url:https://uceva.datasae.co/siga_new/web/app.php/publicomanejoespacios}")
    private String urlBase;
    /**
     * Esta tarea se ejecutará automáticamente
     * Cron: Segundo Minuto Hora Día Mes Día_de_Semana
     * "0 0 3 * * ?" -> Todos los días a las 3:00 AM.
     * Puedes probarlo rápido usando: fixedDelay = 60000 (Ejecuta cada minuto).
     */

    //@Scheduled(cron = "0 0 3 * * ?") // 3 AM todos los días
    @Scheduled(initialDelay = 2000, fixedDelay = 600000)
    @Transactional
    public void sincronizarAulas() {

        URI urlSiga = UriComponentsBuilder
                .fromHttpUrl(urlBase)
                .path("/listarAulasPublico")
                .queryParam("_dc", System.currentTimeMillis())
                .queryParam("tipo_aula", "")
                .queryParam("asistencia", "")
                .queryParam("recursos", "")
                .queryParam("query", "")
                .queryParam("page", 1)
                .queryParam("start", 0)
                .queryParam("limit", 150)
                .queryParam("filter", "[{\"property\":\"view\"}]")
                .build()
                .toUri();
        log.debug("URL construida: {}", urlSiga);

        log.info("Iniciando sincronización de aulas desde el sistema de la Universidad...");

        try {
            // Llamar a la API del SIGA
            ResponseEntity<SigaResponseDTO> response = restClient.get()
                    .uri(urlSiga)
                    .retrieve()
                    .toEntity(SigaResponseDTO.class);
            //validar respuesta existosa y comprobar si tiene cuerpo
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                List<AulaExternaDto> aulasSiga = response.getBody().getData();

                if (aulasSiga != null) {
                    for (AulaExternaDto aula : aulasSiga) {
                        //se busca si el aula por codigo Aula(siga) ya existe
                        Optional<Aula> aulaBD = Optional.ofNullable(aulaRepository.findByCodigoAula(aula.getCodigoAula())
                                .orElse(null));

                        Aula aulaLocal;

                        if (aulaBD.isPresent()) {
                            aulaLocal = aulaBD.get();
                        } else {
                            aulaLocal = new Aula();
                        }
                        // Mapeamos los datos
                        aulaLocal.setCodigoAula(aula.getCodigoAula());
                        aulaLocal.setNombreAula(aula.getNombreAula() != null ? aula.getNombreAula() : "Aula sin nombre");
                        aulaLocal.setCodigoEdificio(aula.getCodigoEdificio());
                        aulaLocal.setNombreEdificio(aula.getNombreEdificio());
                        aulaLocal.setCapacidad(aula.getCapacidad() != null ? aula.getCapacidad() : 0);
                        aulaLocal.setCodigoDependencia(aula.getCodigoDependencia());
                        aulaLocal.setNombreDependencia(aula.getNombreDependencia());
                        aulaLocal.setCodigoTipoAula(aula.getCodigoTipoAula());
                        aulaLocal.setNombreTipoAula(aula.getNombreTipoAula());

                        // logica para las aulas especiales que requieren autorizacion
                        // Bloqueamos los códigos: 5 (Salas), 25 (Laboratorios), 80 (Escenarios Deportivos)
                        String tipo = aula.getCodigoTipoAula();
                        boolean requierePermiso = "5".equals(tipo) || "25".equals(tipo) || "80".equals(tipo);
                        aulaLocal.setRequiereAutorizacion(requierePermiso);
                        aulaRepository.save(aulaLocal);
                    }
                }
                log.info("Sincronización finalizada exitosamente. Total procesadas: {}", aulasSiga.size());
            }

        } catch (Exception e) {
            // Manejar si la universidad cae para que el scheduler no bloquee toda la app
            log.error("Ocurrió un error al sincronizar con la Universidad: {}", e.getMessage());
        }

    }
}
