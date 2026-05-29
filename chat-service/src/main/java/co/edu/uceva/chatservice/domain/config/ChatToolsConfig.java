package co.edu.uceva.chatservice.domain.config;

import co.edu.uceva.chatservice.domain.FeignClients.IAulaServiceClient;
import co.edu.uceva.chatservice.domain.FeignClients.IReservaServiceClient;
import co.edu.uceva.chatservice.domain.dto.AulaDTO;
import co.edu.uceva.chatservice.domain.dto.BloqueDTO;
import co.edu.uceva.chatservice.domain.dto.ResponseAulaDTO;
import co.edu.uceva.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatToolsConfig {

    private final IAulaServiceClient aulaClient;
    private final IReservaServiceClient reservaClient;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private List<BloqueDTO> cacheBloques = new ArrayList<>();

    private static final String MSG_ERROR_RED = "Lo siento, tuve un problema al conectar con el sistema. Por favor, intenta de nuevo en unos momentos.";
    private static final String MSG_ERROR_GENERICO = "Lo siento, ocurrio un error inesperado. Por favor, intenta de nuevo en unos momentos.";
    private static final String MSG_SUGERENCIA = " Te gustaria que busque en otro bloque o por tipo de aula (Laboratorio, Aula Interactiva, etc.)?";
    private static final String MSG_ERROR_OCUPADAS_NULL = "El sistema de reservas no respondio correctamente al consultar disponibilidad.";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatToolsConfig(IAulaServiceClient aulaClient, IReservaServiceClient reservaClient,
                           ObjectMapper objectMapper, JwtUtil jwtUtil) {
        this.aulaClient = aulaClient;
        this.reservaClient = reservaClient;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
    }

    private void cargarBloquesSiNecesario() {
        if (cacheBloques != null && !cacheBloques.isEmpty()) {
            return;
        }
        log.info("[ChatToolsConfig] Inicializando. Timezone: {}, Hora actual: {}",
                java.util.TimeZone.getDefault().getID(), java.time.LocalDateTime.now());
        try {
            Map<String, Object> response = aulaClient.listarBloques();
            Object bloquesRaw = response != null ? response.get("bloques") : null;
            if (bloquesRaw != null) {
                cacheBloques = objectMapper.convertValue(bloquesRaw,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, BloqueDTO.class));
                log.info("[ChatToolsConfig] {} bloques precargados en cache.", cacheBloques.size());
            }
        } catch (Exception e) {
            log.warn("[ChatToolsConfig] No se pudieron precargar bloques (aula-service no disponible?): {}", e.getMessage());
        }
    }

    private void validarFechas(String fecha, String horaInicio, String horaFin) throws IllegalArgumentException {
        try {
            LocalDate.parse(fecha, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha '" + fecha + "' no tiene el formato valido (yyyy-MM-dd).");
        }
        try {
            LocalTime.parse(horaInicio, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora de inicio '" + horaInicio + "' no tiene el formato valido (HH:mm).");
        }
        try {
            LocalTime.parse(horaFin, TIME_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora de fin '" + horaFin + "' no tiene el formato valido (HH:mm).");
        }
        LocalTime inicio = LocalTime.parse(horaInicio, TIME_FMT);
        LocalTime fin = LocalTime.parse(horaFin, TIME_FMT);
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La hora de inicio (" + horaInicio + ") debe ser anterior a la hora de fin (" + horaFin + ").");
        }
    }

    private String resolverFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return LocalDate.now().format(DATE_FMT);
        try {
            LocalDate.parse(fecha, DATE_FMT);
            return fecha;
        } catch (DateTimeParseException e) {
            log.warn("[ChatToolsConfig] Fecha invalida '{}', usando hoy", fecha);
            return LocalDate.now().format(DATE_FMT);
        }
    }

    private String resolverHora(String hora, String defaultHora) {
        if (hora == null || hora.isBlank()) return defaultHora;
        try {
            LocalTime.parse(hora, TIME_FMT);
            return hora;
        } catch (DateTimeParseException e) {
            log.warn("[ChatToolsConfig] Hora invalida '{}', usando {}", hora, defaultHora);
            return defaultHora;
        }
    }

    private String normalizar(String input) {
        if (input == null) return "";
        String sinAcentos = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toUpperCase().trim().replaceAll("\\s+", " ");
    }

    private int distanciaLevenshtein(String a, String b) {
        int m = a.length();
        int n = b.length();
        if (m == 0) return n;
        if (n == 0) return m;
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[m][n];
    }

    private BloqueDTO resolverBloque(String inputBloque) {
        cargarBloquesSiNecesario();
        try {
            Map<String, BloqueDTO> responseMap = aulaClient.buscarBloque(inputBloque);
            BloqueDTO bloque = responseMap != null ? responseMap.get("bloque") : null;
            if (bloque != null && bloque.id() != null) {
                return bloque;
            }
        } catch (FeignException e) {
            log.debug("[ChatToolsConfig] Busqueda directa de bloque fallo, usando cache. Status: {}", e.status());
        }

        List<BloqueDTO> todosLosBloques = cacheBloques;
        if (todosLosBloques == null || todosLosBloques.isEmpty()) {
            try {
                Map<String, Object> response = aulaClient.listarBloques();
                Object bloquesRaw = response != null ? response.get("bloques") : null;
                if (bloquesRaw == null) return null;
                todosLosBloques = objectMapper.convertValue(bloquesRaw,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, BloqueDTO.class));
                cacheBloques = todosLosBloques;
            } catch (Exception e) {
                log.error("[ChatToolsConfig] Error cargando bloques desde cache: {}", e.getMessage());
                return null;
            }
        }

        if (todosLosBloques == null || todosLosBloques.isEmpty()) return null;

        String inputNorm = normalizar(inputBloque);
        BloqueDTO mejorCandidato = null;
        int mejorDistancia = Integer.MAX_VALUE;

        for (BloqueDTO b : todosLosBloques) {
            String nombreNorm = normalizar(b.nombre());
            String codigoNorm = normalizar(b.codigoEdificio());
            if (nombreNorm.contains(inputNorm) || inputNorm.contains(nombreNorm) ||
                    codigoNorm.contains(inputNorm) || inputNorm.contains(codigoNorm)) {
                return b;
            }
            int distNombre = distanciaLevenshtein(inputNorm, nombreNorm);
            int distCodigo = distanciaLevenshtein(inputNorm, codigoNorm);
            int distMin = Math.min(distNombre, distCodigo);
            int umbral = Math.max(3, Math.max(inputNorm.length(), nombreNorm.length()) / 3);
            if (distMin <= umbral && distMin < mejorDistancia) {
                mejorDistancia = distMin;
                mejorCandidato = b;
            }
        }
        return mejorCandidato;
    }

    private String listarNombresBloques() {
        cargarBloquesSiNecesario();
        if (cacheBloques != null && !cacheBloques.isEmpty()) {
            return cacheBloques.stream()
                    .map(BloqueDTO::nombre)
                    .filter(n -> n != null)
                    .collect(Collectors.joining(", "));
        }
        try {
            Map<String, Object> response = aulaClient.listarBloques();
            Object bloquesRaw = response != null ? response.get("bloques") : null;
            if (bloquesRaw == null) return "";
            List<BloqueDTO> bloques = objectMapper.convertValue(bloquesRaw,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BloqueDTO.class));
            cacheBloques = bloques;
            return bloques.stream().map(BloqueDTO::nombre).filter(n -> n != null).collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }

    private List<Long> verificarOcupadas(List<Long> ocupadas) {
        if (ocupadas == null) {
            log.error("[ChatToolsConfig] obtenerAulasOcupadas devolvio null - posible fallo en reserva-service");
            throw new RuntimeException(MSG_ERROR_OCUPADAS_NULL);
        }
        return ocupadas;
    }

    private String formatearErrorFeign(String operacion, FeignException e) {
        int status = e.status();
        String body = e.contentUTF8();
        log.error("[ChatToolsConfig] Error Feign en '{}': status={}, body={}", operacion, status, body);
        if (status == 404) {
            return "El recurso solicitado no fue encontrado en el sistema. Verifica los datos e intenta de nuevo.";
        }
        if (status == 409) {
            return "Conflicto detectado: ya existe una reserva en ese horario para esa aula. Elige otro horario.";
        }
        if (status == 400) {
            return "Datos invalidos: " + (body != null && body.length() < 200 ? body : "Verifica los campos ingresados.");
        }
        if (status == 401 || status == 403) {
            return "No tienes permisos para realizar esta operacion. Verifica tu sesion.";
        }
        if (status >= 500) {
            return "El servicio externo esta experimentando problemas (error " + status + "). Intenta de nuevo en unos momentos.";
        }
        return MSG_ERROR_RED;
    }

    // ── Tools ───────────────────────────────────────────────────────────────

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "listarAulasDisponibles", description = "Lista TODAS las aulas disponibles sin necesidad de especificar bloque o tipo. Usa esta tool cuando el usuario pregunte por aulas disponibles de forma general sin indicar un bloque o tipo especifico.")
    public String listarAulasDisponibles(
            @ToolParam(description = "Fecha yyyy-MM-dd (opcional, por defecto hoy)", required = false) String fecha,
            @ToolParam(description = "Hora inicio HH:mm (opcional, por defecto 07:00)", required = false) String horaInicio,
            @ToolParam(description = "Hora fin HH:mm (opcional, por defecto 19:00)", required = false) String horaFin
    ) {
        log.info("[listarAulasDisponibles] INICIO - fecha={}, horaInicio={}, horaFin={}", fecha, horaInicio, horaFin);
        try {
            String f = resolverFecha(fecha);
            String hi = resolverHora(horaInicio, "07:00");
            String hf = resolverHora(horaFin, "19:00");

            try {
                validarFechas(f, hi, hf);
            } catch (IllegalArgumentException e) {
                return "Error en los parametros: " + e.getMessage();
            }

            log.info("[listarAulasDisponibles] Consultando todas las aulas...");
            Map<String, Object> todasResponse = aulaClient.listarAulas();
            if (todasResponse == null || !todasResponse.containsKey("aulas")) {
                log.warn("[listarAulasDisponibles] aula-service no devolvio datos validos");
                return "No se pudieron obtener las aulas del sistema. Intenta de nuevo en unos momentos.";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> todasRaw = (List<Map<String, Object>>) todasResponse.get("aulas");
            if (todasRaw == null || todasRaw.isEmpty()) {
                return "No hay aulas registradas en el sistema.";
            }

            List<AulaDTO> todas = todasRaw.stream()
                    .map(m -> objectMapper.convertValue(m, AulaDTO.class))
                    .collect(Collectors.toList());
            log.info("[listarAulasDisponibles] Total aulas en sistema: {}", todas.size());

            List<Long> ocupadas;
            try {
                log.info("[listarAulasDisponibles] Consultando aulas ocupadas en reserva-service...");
                ocupadas = verificarOcupadas(reservaClient.obtenerAulasOcupadas(f, hi, hf));
                log.info("[listarAulasDisponibles] Aulas ocupadas: {}", ocupadas.size());
            } catch (Exception e) {
                log.error("[listarAulasDisponibles] Error consultando ocupadas: {}", e.getMessage());
                return MSG_ERROR_RED + " No se pudo verificar la disponibilidad con el sistema de reservas.";
            }

            List<AulaDTO> disponibles = todas.stream()
                    .filter(a -> !ocupadas.contains(a.id()))
                    .collect(Collectors.toList());
            log.info("[listarAulasDisponibles] Aulas disponibles despues de filtro: {}/{}", disponibles.size(), todas.size());

            // Log primeros IDs de cada conjunto para diagnostico
            List<Long> idsAulas = todas.stream().map(AulaDTO::id).limit(10).collect(Collectors.toList());
            List<Long> idsOcupadas = ocupadas.stream().limit(10).collect(Collectors.toList());
            log.info("[listarAulasDisponibles] Primeros 10 IDs de aulas: {}", idsAulas);
            log.info("[listarAulasDisponibles] Primeros 10 IDs ocupados: {}", idsOcupadas);
            // Buscar B109 en los datos para verificacion
            todas.stream()
                .filter(a -> a.nombreAula() != null && a.nombreAula().toUpperCase().contains("B109"))
                .findFirst()
                .ifPresent(a -> log.info("[listarAulasDisponibles] B109: id={}, nombre={}, ocupado={}",
                    a.id(), a.nombreAula(), ocupadas.contains(a.id())));

            if (disponibles.isEmpty()) {
                return "No hay aulas disponibles el " + f + " de " + hi + " a " + hf
                        + ". Todas las aulas estan ocupadas en ese horario." + MSG_SUGERENCIA;
            }

            Map<String, List<AulaDTO>> agrupadas = disponibles.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.bloque() != null ? a.bloque().nombre() : "Sin bloque",
                            java.util.LinkedHashMap::new, Collectors.toList()));

            StringBuilder sb = new StringBuilder("**Aulas disponibles** el ")
                    .append(f).append(" de ").append(hi).append(" a ").append(hf).append(":\n\n");
            sb.append("| Bloque | Aula | Codigo | Capacidad |\n");
            sb.append("|--------|------|--------|------------|\n");
            for (var entry : agrupadas.entrySet()) {
                for (AulaDTO a : entry.getValue()) {
                    sb.append("| ").append(entry.getKey())
                            .append(" | ").append(a.nombreAula())
                            .append(" | ").append(a.codigoAula())
                            .append(" | ").append(a.capacidad()).append(" |\n");
                }
            }
            sb.append("\n**Total:** ").append(disponibles.size())
                    .append(" aulas disponibles de ").append(todas.size()).append(" registradas.");
            log.info("[listarAulasDisponibles] OK - {} disponibles en {} bloques", disponibles.size(), agrupadas.size());
            return sb.toString();
        } catch (FeignException e) {
            return formatearErrorFeign("listarAulasDisponibles", e);
        } catch (Exception e) {
            log.error("[listarAulasDisponibles] Error inesperado", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "consultarPorBloque", description = "Consulta aulas disponibles en un bloque especifico.")
    public String consultarPorBloque(
            @ToolParam(description = "Nombre del bloque, ej: Bloque B") String bloque,
            @ToolParam(description = "Fecha yyyy-MM-dd") String fecha,
            @ToolParam(description = "Hora inicio HH:mm") String horaInicio,
            @ToolParam(description = "Hora fin HH:mm") String horaFin
    ) {
        log.info("[consultarPorBloque] INICIO - bloque={}, fecha={}, inicio={}, fin={}", bloque, fecha, horaInicio, horaFin);
        try {
            try {
                validarFechas(fecha, horaInicio, horaFin);
            } catch (IllegalArgumentException e) {
                return "Error en los parametros: " + e.getMessage();
            }

            BloqueDTO bloqueDTO = resolverBloque(bloque);
            if (bloqueDTO == null || bloqueDTO.id() == null) {
                String nombres = listarNombresBloques();
                return "No encontre ningun bloque con el nombre '" + bloque + "'."
                        + (nombres.isEmpty() ? "" : " Los bloques disponibles son: " + nombres + ".") + MSG_SUGERENCIA;
            }
            log.info("[consultarPorBloque] Bloque resuelto: {} (id={})", bloqueDTO.nombre(), bloqueDTO.id());

            ResponseAulaDTO responseAula = aulaClient.listarAulasPorBloque(bloqueDTO.id());
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "El bloque " + bloqueDTO.nombre() + " no tiene aulas registradas." + MSG_SUGERENCIA;
            }
            log.info("[consultarPorBloque] Aulas en bloque: {}", responseAula.aulas().size());

            List<Long> ocupadas;
            try {
                ocupadas = verificarOcupadas(reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin));
                log.info("[consultarPorBloque] Aulas ocupadas: {}", ocupadas.size());
            } catch (Exception e) {
                log.error("[consultarPorBloque] Error consultando ocupadas: {}", e.getMessage());
                return MSG_ERROR_RED + " No se pudo verificar la disponibilidad con el sistema de reservas.";
            }

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> !ocupadas.contains(a.id()))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                return "No hay aulas disponibles en el bloque " + bloqueDTO.nombre()
                        + " el " + fecha + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }

            StringBuilder sb = new StringBuilder("**Aulas disponibles en ").append(bloqueDTO.nombre())
                    .append("** el ").append(fecha).append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula())
                    .append(" (Capacidad: ").append(a.capacidad()).append(")\n"));
            log.info("[consultarPorBloque] OK - {} disponibles", disponibles.size());
            return sb.toString();
        } catch (FeignException e) {
            return formatearErrorFeign("consultarPorBloque", e);
        } catch (Exception e) {
            log.error("[consultarPorBloque] Error inesperado", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "consultarPorTipo", description = "Consulta aulas disponibles por tipo (Laboratorio, Aula Interactiva, etc).")
    public String consultarPorTipo(
            @ToolParam(description = "Tipo de aula") String tipoAula,
            @ToolParam(description = "Fecha yyyy-MM-dd") String fecha,
            @ToolParam(description = "Hora inicio HH:mm") String horaInicio,
            @ToolParam(description = "Hora fin HH:mm") String horaFin
    ) {
        log.info("[consultarPorTipo] INICIO - tipo={}, fecha={}, inicio={}, fin={}", tipoAula, fecha, horaInicio, horaFin);
        try {
            try {
                validarFechas(fecha, horaInicio, horaFin);
            } catch (IllegalArgumentException e) {
                return "Error en los parametros: " + e.getMessage();
            }

            ResponseAulaDTO responseAula = aulaClient.listarAulasPorTipoAula(tipoAula);
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "No encontre aulas del tipo '" + tipoAula + "'." + MSG_SUGERENCIA;
            }
            log.info("[consultarPorTipo] Aulas del tipo '{}': {}", tipoAula, responseAula.aulas().size());

            List<Long> ocupadas;
            try {
                ocupadas = verificarOcupadas(reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin));
                log.info("[consultarPorTipo] Aulas ocupadas: {}", ocupadas.size());
            } catch (Exception e) {
                log.error("[consultarPorTipo] Error consultando ocupadas: {}", e.getMessage());
                return MSG_ERROR_RED + " No se pudo verificar la disponibilidad.";
            }

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> !ocupadas.contains(a.id()))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                return "No hay aulas de tipo " + tipoAula + " disponibles el " + fecha
                        + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }

            StringBuilder sb = new StringBuilder("**Aulas de tipo ").append(tipoAula)
                    .append("** disponibles el ").append(fecha).append(" de ").append(horaInicio)
                    .append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula())
                    .append(" (Bloque: ").append(a.bloque().nombre())
                    .append(", Capacidad: ").append(a.capacidad()).append(")\n"));
            log.info("[consultarPorTipo] OK - {} disponibles", disponibles.size());
            return sb.toString();
        } catch (FeignException e) {
            return formatearErrorFeign("consultarPorTipo", e);
        } catch (Exception e) {
            log.error("[consultarPorTipo] Error inesperado", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "consultarPorNombreAula", description = "Consulta disponibilidad de un aula especifica por su nombre.")
    public String consultarPorNombreAula(
            @ToolParam(description = "Nombre del aula") String nombreAula,
            @ToolParam(description = "Fecha yyyy-MM-dd") String fecha,
            @ToolParam(description = "Hora inicio HH:mm") String horaInicio,
            @ToolParam(description = "Hora fin HH:mm") String horaFin
    ) {
        log.info("[consultarPorNombreAula] INICIO - aula={}, fecha={}, inicio={}, fin={}", nombreAula, fecha, horaInicio, horaFin);
        try {
            try {
                validarFechas(fecha, horaInicio, horaFin);
            } catch (IllegalArgumentException e) {
                return "Error en los parametros: " + e.getMessage();
            }

            ResponseAulaDTO responseAula = aulaClient.buscarAulasPorNombre(nombreAula);
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "No encontre ninguna aula con el nombre '" + nombreAula + "'." + MSG_SUGERENCIA;
            }

            List<Long> ocupadas;
            try {
                ocupadas = verificarOcupadas(reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin));
            } catch (Exception e) {
                log.error("[consultarPorNombreAula] Error consultando ocupadas: {}", e.getMessage());
                return MSG_ERROR_RED + " No se pudo verificar la disponibilidad.";
            }

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(aula -> !ocupadas.contains(aula.id()))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                return "Lo siento, el aula '" + nombreAula + "' esta ocupada el " + fecha
                        + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }

            StringBuilder sb = new StringBuilder("Buenas noticias! Encontre disponibilidad para '")
                    .append(nombreAula).append("' el ").append(fecha)
                    .append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(aula ->
                    sb.append("- ").append(aula.nombreAula())
                            .append(" (Bloque: ").append(aula.bloque().nombre())
                            .append(", Capacidad: ").append(aula.capacidad()).append(" personas)\n"));
            log.info("[consultarPorNombreAula] OK - {} disponibles", disponibles.size());
            return sb.toString();
        } catch (FeignException e) {
            return formatearErrorFeign("consultarPorNombreAula", e);
        } catch (Exception e) {
            log.error("[consultarPorNombreAula] Error inesperado", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "reservarAulaTool", description = "Reserva un aula especifica.")
    public String reservarAulaTool(
            @ToolParam(description = "Nombre del aula") String nombreAula,
            @ToolParam(description = "Fecha yyyy-MM-dd") String fecha,
            @ToolParam(description = "Hora inicio HH:mm") String horaInicio,
            @ToolParam(description = "Hora fin HH:mm") String horaFin,
            @ToolParam(description = "Motivo o titulo de la reserva") String motivo,
            @ToolParam(description = "Codigo del programa academico (opcional)", required = false) String codigoPrograma
    ) {
        log.info("[reservarAulaTool] INICIO - aula={}, fecha={}, {}-{}, motivo={}", nombreAula, fecha, horaInicio, horaFin, motivo);

        try {
            validarFechas(fecha, horaInicio, horaFin);
        } catch (IllegalArgumentException e) {
            return "Error en los parametros: " + e.getMessage();
        }

        if (motivo == null || motivo.isBlank()) {
            return "ERROR: El motivo de la reserva es obligatorio. INSTRUCCION PARA EL ASISTENTE: Pide al usuario el motivo o titulo de la reserva.";
        }

        Long aulaId = null;
        AulaDTO aulaEncontrada = null;
        try {
            ResponseAulaDTO response = aulaClient.buscarAulasPorNombre(nombreAula);
            if (response == null || response.aulas() == null || response.aulas().isEmpty()) {
                return "ERROR: No encontre ninguna aula con el nombre '" + nombreAula + "'. "
                        + "INSTRUCCION PARA EL ASISTENTE: Pide al usuario que verifique el nombre del aula. "
                        + "NO inventes nombres de aulas. NO busques otra aula sin permiso explicito del usuario.";
            }
            aulaEncontrada = response.aulas().get(0);
            aulaId = aulaEncontrada.id();
            log.info("[reservarAulaTool] Aula resuelta: id={}, codigoSiga={}", aulaId, aulaEncontrada.codigoAula());
        } catch (FeignException e) {
            log.error("[reservarAulaTool] Error buscando aula: status={}", e.status());
            return formatearErrorFeign("busqueda de aula", e);
        } catch (Exception e) {
            log.error("[reservarAulaTool] Error inesperado buscando aula", e);
            return MSG_ERROR_GENERICO;
        }

        String jwtHeader = AuthContext.getJwt();
        if (jwtHeader == null || !jwtHeader.startsWith("Bearer ")) {
            log.warn("[reservarAulaTool] JWT no disponible en AuthContext");
            return "No se pudo identificar tu sesion. Por favor inicia sesion e intenta de nuevo.";
        }

        String jwt = jwtHeader.substring(7);
        if (!jwtUtil.isTokenValid(jwt)) {
            return "Tu sesion ha expirado. Por favor inicia sesion de nuevo.";
        }

        String codigoStr = jwtUtil.extractCodigo(jwt);
        String rolStr = jwtUtil.extractRol(jwt);
        if (codigoStr == null || codigoStr.isBlank()) {
            return "No se pudo determinar tu ID de usuario desde el token JWT.";
        }

        Long idSolicitante;
        try {
            idSolicitante = Long.valueOf(codigoStr);
        } catch (NumberFormatException e) {
            return "No se pudo determinar tu ID de usuario desde el token JWT.";
        }

        String nombreUsuario = jwtUtil.extractNombre(jwt);
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            nombreUsuario = codigoStr;
        }
        if (rolStr == null || rolStr.isBlank()) {
            rolStr = "DOCENTE";
        }

        log.info("[reservarAulaTool] Usuario: id={}, rol={}, nombre={}", idSolicitante, rolStr, nombreUsuario);

        if ("ESTUDIANTE".equalsIgnoreCase(rolStr)) {
            String codigoTipoAula = (aulaEncontrada != null && aulaEncontrada.tipoAula() != null)
                    ? aulaEncontrada.tipoAula().codigoTipoAula() : null;
            if (!"78".equals(codigoTipoAula) && !"79".equals(codigoTipoAula)) {
                log.info("[reservarAulaTool] Estudiante intento reservar aula tipo {}", codigoTipoAula);
                return "ERROR: Como estudiante, solo puedes reservar aulas interactivas (tipo 78) o audiovisuales (tipo 79). "
                        + "El aula '" + nombreAula + "' es de tipo "
                        + (codigoTipoAula != null ? codigoTipoAula : "desconocido") + ". "
                        + "INSTRUCCION PARA EL ASISTENTE: NO sugieras otra aula automaticamente. "
                        + "Pregunta al usuario si quiere ver aulas de tipo 78 o 79 disponibles.";
            }
        }

        try {
            log.info("[reservarAulaTool] Verificando disponibilidad...");
            List<Long> ocupadas = verificarOcupadas(reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin));
            if (ocupadas.contains(aulaId)) {
                log.info("[reservarAulaTool] Aula {} ocupada en el horario solicitado", aulaId);
                return "ERROR: El aula '" + nombreAula + "' ya esta ocupada el " + fecha
                        + " de " + horaInicio + " a " + horaFin + ". "
                        + "INSTRUCCION PARA EL ASISTENTE: Informa al usuario que esta aula NO esta disponible. "
                        + "NO busques otra aula automaticamente. Pregunta al usuario si quiere buscar otra aula o elegir otro horario.";
            }
            log.info("[reservarAulaTool] Aula disponible, procediendo a crear reserva...");
        } catch (Exception e) {
            log.error("[reservarAulaTool] Error verificando disponibilidad: {}", e.getMessage());
            return MSG_ERROR_RED;
        }

        try {
            String horaInicioISO = fecha + "T" + horaInicio + ":00";
            String horaFinISO = fecha + "T" + horaFin + ":00";

            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("aulaId", aulaId);
            payload.put("horaInicio", horaInicioISO);
            payload.put("horaFin", horaFinISO);
            payload.put("estado", "CONFIRMADA");
            payload.put("idSolicitante", idSolicitante);
            payload.put("rolSolicitante", rolStr.toUpperCase());
            payload.put("nombreUsuarioResponsable", nombreUsuario);
            payload.put("titulo", motivo);
            if (codigoPrograma != null && !codigoPrograma.isBlank()) {
                payload.put("codigoPrograma", codigoPrograma);
            }

            log.info("[reservarAulaTool] Enviando payload a reserva-service");
            Map<String, Object> respuesta = reservaClient.crearReserva(payload);

            String estadoFinal = "CONFIRMADA";
            Object reservaCreada = respuesta != null ? respuesta.get("reserva") : null;
            if (reservaCreada instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reservaMap = (Map<String, Object>) reservaCreada;
                Object estadoObj = reservaMap.get("estado");
                if (estadoObj != null) estadoFinal = estadoObj.toString();
            }

            log.info("[reservarAulaTool] Reserva creada con estado: {}", estadoFinal);

            if ("PENDIENTE".equalsIgnoreCase(estadoFinal)) {
                return "Tu reserva para el aula '" + nombreAula
                        + "' fue creada y quedo **PENDIENTE de autorizacion**. Motivo: " + motivo + ". "
                        + "El administrador debe aprobarla. Te notificaremos cuando sea revisada. "
                        + "Fecha: " + fecha + " de " + horaInicio + " a " + horaFin + ".";
            } else {
                return "Tu reserva fue **CONFIRMADA** exitosamente! El aula '" + nombreAula
                        + "' ha sido reservada el " + fecha + " de " + horaInicio + " a " + horaFin
                        + " para: " + motivo + ".";
            }
        } catch (FeignException e) {
            return formatearErrorFeign("creacion de reserva", e);
        } catch (Exception e) {
            log.error("[reservarAulaTool] Error inesperado al crear reserva", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @Retryable(retryFor = {FeignException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @Tool(name = "consultarHorariosAula", description = "Consulta horarios ocupados y disponibles de un aula.")
    public String consultarHorariosAula(
            @ToolParam(description = "Nombre del aula") String nombreAula,
            @ToolParam(description = "Fecha yyyy-MM-dd") String fecha
    ) {
        log.info("[consultarHorariosAula] INICIO - aula={}, fecha={}", nombreAula, fecha);
        try {
            ResponseAulaDTO response = aulaClient.buscarAulasPorNombre(nombreAula);
            if (response == null || response.aulas() == null || response.aulas().isEmpty()) {
                return "No encontre ninguna aula con el nombre '" + nombreAula + "'. Verifica el nombre e intenta de nuevo.";
            }
            Long aulaId = response.aulas().get(0).id();
            String nombreReal = response.aulas().get(0).nombreAula();
            log.info("[consultarHorariosAula] Aula resuelta: {} (id={})", nombreReal, aulaId);

            Map<String, Object> respuestaReservas = reservaClient.obtenerReservasPorAula(aulaId);
            List<Map<String, Object>> reservas = extraerReservas(respuestaReservas);

            java.time.LocalDate fechaConsulta = java.time.LocalDate.parse(fecha);
            List<String[]> ocupados = new ArrayList<>();

            for (Map<String, Object> reserva : reservas) {
                String inicioStr = String.valueOf(reserva.get("horaInicio"));
                String finStr = String.valueOf(reserva.get("horaFin"));
                if (inicioStr == null || finStr == null || "null".equals(inicioStr)) continue;
                java.time.LocalDateTime inicio = parsearFechaHora(inicioStr);
                java.time.LocalDateTime fin = parsearFechaHora(finStr);
                if (inicio != null && fin != null && inicio.toLocalDate().equals(fechaConsulta)) {
                    ocupados.add(new String[]{inicio.toLocalTime().toString(), fin.toLocalTime().toString()});
                }
            }

            log.info("[consultarHorariosAula] {} bloques ocupados encontrados", ocupados.size());

            StringBuilder sb = new StringBuilder();
            sb.append("Horarios del aula **").append(nombreReal).append("** el **").append(fecha).append("**:\n\n");
            if (ocupados.isEmpty()) {
                sb.append("El aula esta completamente libre ese dia.\n");
            } else {
                sb.append("**Horarios OCUPADOS:**\n");
                for (String[] r : ocupados) {
                    sb.append("  * ").append(r[0]).append(" - ").append(r[1]).append("\n");
                }
            }

            sb.append("\n**Horarios sugeridos (libres):**\n");
            int[][] bloques = {{7, 9}, {9, 11}, {11, 13}, {13, 15}, {15, 17}, {17, 19}};
            for (int[] bloque : bloques) {
                String hInicio = String.format("%02d:00", bloque[0]);
                String hFin = String.format("%02d:00", bloque[1]);
                if (!estaOcupado(hInicio, hFin, ocupados)) {
                    sb.append("  * ").append(hInicio).append(" - ").append(hFin).append(" \n");
                } else {
                    sb.append("  * ").append(hInicio).append(" - ").append(hFin).append("  Ocupado\n");
                }
            }
            sb.append("\nSi quieres reservar uno de estos horarios, dimelo y lo agendo.");
            log.info("[consultarHorariosAula] OK");
            return sb.toString();
        } catch (FeignException e) {
            return formatearErrorFeign("consultarHorariosAula", e);
        } catch (Exception e) {
            log.error("[consultarHorariosAula] Error inesperado", e);
            return MSG_ERROR_GENERICO;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extraerReservas(Map<String, Object> respuesta) {
        if (respuesta == null) return List.of();
        Object reservasObj = respuesta.get("reservas");
        if (reservasObj instanceof List) {
            return (List<Map<String, Object>>) reservasObj;
        }
        return List.of();
    }

    private java.time.LocalDateTime parsearFechaHora(String valor) {
        try {
            String limpio = valor.replace(" ", "T");
            if (limpio.contains(".")) limpio = limpio.substring(0, limpio.indexOf('.'));
            return java.time.LocalDateTime.parse(limpio);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean estaOcupado(String inicio, String fin, List<String[]> ocupados) {
        java.time.LocalTime tInicio = java.time.LocalTime.parse(inicio);
        java.time.LocalTime tFin = java.time.LocalTime.parse(fin);
        for (String[] o : ocupados) {
            java.time.LocalTime oInicio = java.time.LocalTime.parse(o[0]);
            java.time.LocalTime oFin = java.time.LocalTime.parse(o[1]);
            if (tInicio.isBefore(oFin) && tFin.isAfter(oInicio)) {
                return true;
            }
        }
        return false;
    }
}
