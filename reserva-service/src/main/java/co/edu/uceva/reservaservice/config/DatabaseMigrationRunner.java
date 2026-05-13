package co.edu.uceva.reservaservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Migrador de base de datos para el reserva-service.
 * Maneja cambios estructurales que JPA/Hibernate no gestiona automáticamente:
 * - Renombrado de codigo_aula → aula_id
 * - Agregado de columnas nuevas (rol_solicitante, nombre_usuario_responsable)
 * - Recreación del constraint GIST de no solapamiento
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[DB Migration] Iniciando migración de base de datos...");

        // 1. Migrar columna principal: codigo_aula → aula_id
        migrarColumnaAulaId();

        // 2. Agregar columnas nuevas (rol_solicitante, nombre_usuario_responsable)
        agregarColumnasNuevas();

        // 3. Recrear constraint GIST de no solapamiento sobre aula_id
        recrearConstraintGist();

        log.info("[DB Migration] Migración completada exitosamente.");
    }

    private void migrarColumnaAulaId() {
        boolean existeAulaId = columnaExiste("reservas", "aula_id");
        boolean existeCodigoAula = columnaExiste("reservas", "codigo_aula");

        if (existeAulaId) {
            log.info("[DB Migration] Columna 'aula_id' ya existe.");
        } else if (existeCodigoAula) {
            log.info("[DB Migration] Renombrando 'codigo_aula' → 'aula_id'...");
            jdbcTemplate.execute("ALTER TABLE reservas RENAME COLUMN codigo_aula TO aula_id");
            log.info("[DB Migration] Columna renombrada exitosamente.");
        } else {
            log.info("[DB Migration] Creando columna 'aula_id'...");
            jdbcTemplate.execute("ALTER TABLE reservas ADD COLUMN aula_id BIGINT NOT NULL DEFAULT 0");
            log.info("[DB Migration] Columna 'aula_id' creada.");
        }
    }

    private void agregarColumnasNuevas() {
        List<Map<String, String>> columnas = List.of(
                Map.of("nombre", "rol_solicitante", "tipo", "VARCHAR(255)"),
                Map.of("nombre", "nombre_usuario_responsable", "tipo", "VARCHAR(255)"),
                Map.of("nombre", "titulo", "tipo", "VARCHAR(255)")
        );

        for (Map<String, String> columna : columnas) {
            String nombre = columna.get("nombre");
            String tipo = columna.get("tipo");
            if (!columnaExiste("reservas", nombre)) {
                log.info("[DB Migration] Columna '{}' no encontrada. Creándola...", nombre);
                jdbcTemplate.execute("ALTER TABLE reservas ADD COLUMN IF NOT EXISTS " + nombre + " " + tipo);
                log.info("[DB Migration] Columna '{}' creada exitosamente.", nombre);
            } else {
                log.info("[DB Migration] Columna '{}' ya existe.", nombre);
            }
        }
    }

    private void recrearConstraintGist() {
        log.info("[DB Migration] Verificando constraint GIST 'no_solapamiento_reservas'...");
        try {
            // Eliminar constraint existente (puede estar en codigo_aula antiguo)
            jdbcTemplate.execute("ALTER TABLE reservas DROP CONSTRAINT IF EXISTS no_solapamiento_reservas");
            log.info("[DB Migration] Constraint anterior eliminado.");

            // Crear nuevo constraint sobre aula_id
            jdbcTemplate.execute(
                "ALTER TABLE reservas ADD CONSTRAINT no_solapamiento_reservas " +
                "EXCLUDE USING gist (aula_id WITH =, tsrange(hora_inicio, hora_fin) WITH &&) " +
                "WHERE (estado != 'CANCELADA')"
            );
            log.info("[DB Migration] Constraint GIST recreado exitosamente sobre 'aula_id'.");
        } catch (Exception e) {
            log.error("[DB Migration] Error recreando constraint GIST: {}", e.getMessage());
            log.warn("[DB Migration] ¡ADVERTENCIA! El sistema puede no proteger contra solapamientos de reservas.");
        }
    }

    private boolean columnaExiste(String tabla, String columna) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tabla, columna);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("[DB Migration] Error verificando columna {}.{}: {}", tabla, columna, e.getMessage());
            return false;
        }
    }
}
