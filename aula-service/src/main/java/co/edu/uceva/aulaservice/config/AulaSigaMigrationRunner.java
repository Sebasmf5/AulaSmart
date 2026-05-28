package co.edu.uceva.aulaservice.config;

import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.repository.IAulaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migración automática para aulas existentes que no tienen el campo sincronizadaConSiga.
 * Al agregar la columna nueva, las aulas previas quedan con NULL.
 * Este runner las marca como true (asumiendo que las aulas existentes provienen de SIGA
 * o ya estaban operativas antes del cambio), garantizando que el chatbot y el reserva-service
 * sigan consultando SIGA para ellas sin interrupciones.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AulaSigaMigrationRunner implements CommandLineRunner {

    private final IAulaRepository aulaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Aula> aulasSinSincronizar = aulaRepository.findAll().stream()
                .filter(a -> a.getSincronizadaConSiga() == null)
                .toList();

        if (!aulasSinSincronizar.isEmpty()) {
            log.info("[Migración] Se encontraron {} aulas sin valor en 'sincronizadaConSiga'. Marcándolas como true...", aulasSinSincronizar.size());
            for (Aula aula : aulasSinSincronizar) {
                aula.setSincronizadaConSiga(true);
            }
            aulaRepository.saveAll(aulasSinSincronizar);
            log.info("[Migración] {} aulas actualizadas correctamente.", aulasSinSincronizar.size());
        } else {
            log.info("[Migración] Todas las aulas ya tienen definido el valor de 'sincronizadaConSiga'.");
        }
    }
}
