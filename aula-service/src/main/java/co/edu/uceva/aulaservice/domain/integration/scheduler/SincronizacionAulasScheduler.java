package co.edu.uceva.aulaservice.domain.integration.scheduler;

import co.edu.uceva.aulaservice.domain.integration.SigaDTO.AulaExternaDto;
import co.edu.uceva.aulaservice.domain.integration.SigaDTO.SigaResponseDTO;
import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.model.Bloque;
import co.edu.uceva.aulaservice.domain.model.Facultad;
import co.edu.uceva.aulaservice.domain.model.TipoAula;
import co.edu.uceva.aulaservice.domain.repository.IAulaRepository;
import co.edu.uceva.aulaservice.domain.repository.IBloqueRepository;
import co.edu.uceva.aulaservice.domain.repository.IFacultadRepository;
import co.edu.uceva.aulaservice.domain.repository.ITipoAulaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionAulasScheduler {

    private final RestClient restClient;
    private final IAulaRepository aulaRepository;
    private final IBloqueRepository bloqueRepository;
    private final IFacultadRepository facultadRepository;
    private final ITipoAulaRepository tipoAulaRepository;

    @Value("${siga.api.url:https://uceva.datasae.co/siga_new/web/app.php/publicomanejoespacios}")
    private String urlBase;

    /**
     * Esta tarea se ejecutará automáticamente.
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
            ResponseEntity<SigaResponseDTO> response = restClient.get()
                    .uri(urlSiga)
                    .retrieve()
                    .toEntity(SigaResponseDTO.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                List<AulaExternaDto> aulasSiga = response.getBody().getData();

                if (aulasSiga != null) {
                    for (AulaExternaDto aula : aulasSiga) {

                        // ── 1. Upsert Facultad (codigoDependencia del SIGA) ──────────────────
                        Facultad facultad = facultadRepository
                                .findByCodigoDependencia(aula.getCodigoDependencia())
                                .orElseGet(() -> {
                                    Facultad f = new Facultad();
                                    f.setCodigoDependencia(aula.getCodigoDependencia());
                                    f.setNombre(aula.getNombreDependencia());
                                    return facultadRepository.save(f);
                                });

                        // ── 2. Upsert Bloque (codigoEdificio del SIGA) ───────────────────────
                        Bloque bloque = bloqueRepository
                                .findByCodigoEdificio(aula.getCodigoEdificio())
                                .orElseGet(() -> {
                                    Bloque b = new Bloque();
                                    b.setCodigoEdificio(aula.getCodigoEdificio());
                                    b.setNombre(aula.getNombreEdificio());
                                    return bloqueRepository.save(b);
                                });

                        boolean hasFacultad = bloque.getFacultades().stream()
                                .anyMatch(f -> f.getCodigoDependencia().equals(facultad.getCodigoDependencia()));
                        
                        if (!hasFacultad) {
                            bloque.getFacultades().add(facultad);
                            bloque = bloqueRepository.save(bloque);
                        }

                        // ── 3. Upsert TipoAula (codigoTipoAula del SIGA) ────────────────────
                        TipoAula tipoAula = tipoAulaRepository
                                .findByCodigoTipoAula(aula.getCodigoTipoAula())
                                .orElseGet(() -> {
                                    TipoAula ta = new TipoAula();
                                    ta.setCodigoTipoAula(aula.getCodigoTipoAula());
                                    ta.setNombre(aula.getNombreTipoAula());
                                    
                                    // Aulas especiales que requieren autorización:
                                    // 5 = Salas, 25 = Laboratorios, 80 = Escenarios Deportivos
                                    String tipo = aula.getCodigoTipoAula();
                                    boolean requierePermiso = "5".equals(tipo) || "25".equals(tipo) || "80".equals(tipo);
                                    ta.setRequiereAutorizacion(requierePermiso);
                                    
                                    return tipoAulaRepository.save(ta);
                                });

                        // ── 4. Upsert Aula ───────────────────────────────────────────────────
                        Aula aulaLocal = aulaRepository
                                .findByCodigoAula(aula.getCodigoAula())
                                .orElse(new Aula());

                        aulaLocal.setCodigoAula(aula.getCodigoAula());
                        aulaLocal.setNombreAula(aula.getNombreAula() != null ? aula.getNombreAula() : "Aula sin nombre");
                        aulaLocal.setCapacidad(aula.getCapacidad() != null ? aula.getCapacidad() : 0);
                        aulaLocal.setBloque(bloque);
                        aulaLocal.setTipoAula(tipoAula);

                        aulaRepository.save(aulaLocal);
                    }
                }
                log.info("Sincronización finalizada exitosamente. Total procesadas: {}", aulasSiga != null ? aulasSiga.size() : 0);
            }

        } catch (Exception e) {
            // Si la universidad cae, el scheduler no bloquea toda la app
            log.error("Ocurrió un error al sincronizar con la Universidad: {}", e.getMessage());
        }
    }
}
