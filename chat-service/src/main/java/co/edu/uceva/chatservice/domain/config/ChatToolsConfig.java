package co.edu.uceva.chatservice.domain.config;

import co.edu.uceva.chatservice.domain.FeignClients.IAulaServiceClient;
import co.edu.uceva.chatservice.domain.FeignClients.IReservaServiceClient;
import co.edu.uceva.chatservice.domain.dto.AulaDTO;
import co.edu.uceva.chatservice.domain.dto.BloqueDTO;
import co.edu.uceva.chatservice.domain.dto.ResponseAulaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ai.tool.annotation.Tool;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChatToolsConfig {

    private final IAulaServiceClient aulaClient;
    private final IReservaServiceClient reservaClient;
    private final ObjectMapper objectMapper;

    private static final String MSG_ERROR_RED = "Lo siento, tuve un problema al conectar con el sistema de reservas. Por favor, intenta de nuevo en unos momentos.";
    private static final String MSG_ERROR_GENERICO = "Lo siento, ocurrió un error inesperado. Por favor, intenta de nuevo en unos momentos.";
    private static final String MSG_SUGERENCIA = " ¿Te gustaría que busque en otro bloque o por tipo de aula (Laboratorio, Aula Interactiva, etc.)?";

    public ChatToolsConfig(IAulaServiceClient aulaClient, IReservaServiceClient reservaClient, ObjectMapper objectMapper) {
        this.aulaClient = aulaClient;
        this.reservaClient = reservaClient;
        this.objectMapper = objectMapper;
    }

    // ── Helpers de normalización y fuzzy matching ───────────────────────────

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

    /**
     * Resuelve un bloque a partir de una entrada de usuario usando matching directo,
     * contención mutua y distancia de Levenshtein como fallback.
     */
    private BloqueDTO resolverBloque(String inputBloque) {
        // 1. Intento directo
        try {
            Map<String, BloqueDTO> responseMap = aulaClient.buscarBloque(inputBloque);
            BloqueDTO bloque = responseMap != null ? responseMap.get("bloque") : null;
            if (bloque != null && bloque.id() != null) {
                return bloque;
            }
        } catch (FeignException e) {
            // ignorar, intentaremos fallback
        }

        // 2. Fallback: obtener todos los bloques
        List<BloqueDTO> todosLosBloques;
        try {
            Map<String, Object> response = aulaClient.listarBloques();
            Object bloquesRaw = response != null ? response.get("bloques") : null;
            if (bloquesRaw == null) return null;
            todosLosBloques = objectMapper.convertValue(bloquesRaw,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BloqueDTO.class));
        } catch (Exception e) {
            return null;
        }

        if (todosLosBloques == null || todosLosBloques.isEmpty()) return null;

        String inputNorm = normalizar(inputBloque);
        BloqueDTO mejorCandidato = null;
        int mejorDistancia = Integer.MAX_VALUE;

        for (BloqueDTO b : todosLosBloques) {
            String nombreNorm = normalizar(b.nombre());
            String codigoNorm = normalizar(b.codigoEdificio());

            // Contención mutua (A contiene B o B contiene A)
            if (nombreNorm.contains(inputNorm) || inputNorm.contains(nombreNorm) ||
                    codigoNorm.contains(inputNorm) || inputNorm.contains(codigoNorm)) {
                return b;
            }

            // Levenshtein contra nombre y código
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
        try {
            Map<String, Object> response = aulaClient.listarBloques();
            Object bloquesRaw = response != null ? response.get("bloques") : null;
            if (bloquesRaw == null) return "";
            List<BloqueDTO> bloques = objectMapper.convertValue(bloquesRaw,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, BloqueDTO.class));
            return bloques.stream()
                    .map(BloqueDTO::nombre)
                    .filter(n -> n != null)
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }

    // ── Tools ───────────────────────────────────────────────────────────────

    @Tool(name = "consultarPorBloque", description = "Consulta aulas disponibles en un bloque específico en una fecha y horario.")
    public String consultarPorBloque(
            @ToolParam(description = "Nombre del bloque. Ej: BLOQUE B - AVELLANOS o Abellanos o B") String bloque,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd. Ej: 2026-05-15 o 15 de mayo") String fecha,
            @ToolParam(description = "Hora de inicio en formato HH:mm. Ej: 08:00") String horaInicio,
            @ToolParam(description = "Hora de fin en formato HH:mm. Ej: 10:00") String horaFin
    ) {
        System.out.println("=== [consultarPorBloque] INICIO ===");
        System.out.println("Parametros: bloque=" + bloque + ", fecha=" + fecha + ", inicio=" + horaInicio + ", fin=" + horaFin);
        try {
            BloqueDTO bloqueDTO = resolverBloque(bloque);
            if (bloqueDTO == null || bloqueDTO.id() == null) {
                String nombres = listarNombresBloques();
                return "No encontré ningún bloque con el nombre '" + bloque + "'." +
                        (nombres.isEmpty() ? "" : " Los bloques disponibles son: " + nombres + ".") +
                        MSG_SUGERENCIA;
            }
            System.out.println("Bloque resuelto: " + bloqueDTO.nombre() + " (id=" + bloqueDTO.id() + ")");

            ResponseAulaDTO responseAula = aulaClient.listarAulasPorBloque(bloqueDTO.id());
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "El bloque " + bloqueDTO.nombre() + " no tiene aulas registradas." + MSG_SUGERENCIA;
            }
            System.out.println("Aulas en bloque: " + responseAula.aulas().size());

            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);
            System.out.println("Aulas ocupadas devueltas: " + (ocupadas != null ? ocupadas.size() : "null"));

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> ocupadas == null || !ocupadas.contains(a.codigoAula()))
                    .collect(Collectors.toList());
            if (disponibles.isEmpty()) {
                return "No hay aulas disponibles en el bloque " + bloqueDTO.nombre() + " el " + fecha + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }
            StringBuilder sb = new StringBuilder("Aulas disponibles en ").append(bloqueDTO.nombre())
                    .append(" el ").append(fecha).append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula()).append(" (Capacidad: ").append(a.capacidad()).append(")\n"));
            System.out.println("Retornando " + disponibles.size() + " aulas disponibles");
            return sb.toString();
        } catch (FeignException e) {
            System.err.println("[consultarPorBloque] Error de comunicación: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[consultarPorBloque] Error inesperado: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }
    }

    @Tool(name = "consultarPorTipo", description = "Consulta aulas disponibles por tipo en una fecha y horario.")
    public String consultarPorTipo(
            @ToolParam(description = "Tipo de aula: AULA AUDIOVISUAL, SALA, LABORATORIO, AULA INTERACTIVA") String tipoAula,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd. Ej: 2026-05-15") String fecha,
            @ToolParam(description = "Hora de inicio en formato HH:mm. Ej: 08:00") String horaInicio,
            @ToolParam(description = "Hora de fin en formato HH:mm. Ej: 10:00") String horaFin
    ) {
        try {
            ResponseAulaDTO responseAula = aulaClient.listarAulasPorTipoAula(tipoAula);
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "No encontré aulas del tipo '" + tipoAula + "'." + MSG_SUGERENCIA;
            }
            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);
            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> ocupadas == null || !ocupadas.contains(a.codigoAula()))
                    .collect(Collectors.toList());
            if (disponibles.isEmpty()) {
                return "No hay aulas de tipo " + tipoAula + " disponibles el " + fecha + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }
            StringBuilder sb = new StringBuilder("Aulas de tipo ").append(tipoAula)
                    .append(" disponibles el ").append(fecha).append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula())
                    .append(" (Bloque: ").append(a.bloque().nombre()).append(", Capacidad: ").append(a.capacidad()).append(")\n"));
            return sb.toString();
        } catch (FeignException e) {
            System.err.println("[consultarPorTipo] Error de comunicación: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[consultarPorTipo] Error inesperado: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }
    }

    @Tool(name = "consultarPorNombreAula", description = "Consulta si un aula específica está disponible en una fecha y horario.")
    public String consultarPorNombreAula(
            @ToolParam(description = "Nombre del aula a consultar. Ej: AULA 101") String nombreAula,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd. Ej: 2026-05-15") String fecha,
            @ToolParam(description = "Hora de inicio en formato HH:mm. Ej: 08:00") String horaInicio,
            @ToolParam(description = "Hora de fin en formato HH:mm. Ej: 10:00") String horaFin
    ) {
        try {
            ResponseAulaDTO responseAula = aulaClient.buscarAulasPorNombre(nombreAula);
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                return "No encontré ninguna aula con el nombre '" + nombreAula + "'." + MSG_SUGERENCIA;
            }

            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(aula -> ocupadas == null || !ocupadas.contains(aula.codigoAula()))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                return "Lo siento, el aula '" + nombreAula + "' está ocupada el " + fecha + " de " + horaInicio + " a " + horaFin + "." + MSG_SUGERENCIA;
            }

            StringBuilder sb = new StringBuilder("¡Buenas noticias! Encontré disponibilidad para '")
                    .append(nombreAula).append("' el ").append(fecha)
                    .append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");

            disponibles.forEach(aula ->
                    sb.append("- ").append(aula.nombreAula())
                            .append(" (Bloque: ").append(aula.bloque().nombre())
                            .append(", Capacidad: ").append(aula.capacidad()).append(" personas)\n")
            );

            return sb.toString();
        } catch (FeignException e) {
            System.err.println("[consultarPorNombreAula] Error de comunicación: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[consultarPorNombreAula] Error inesperado: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }
    }

    @Tool(name = "reservarAulaTool", description = "Úsalo SIEMPRE que el usuario dé una orden directa de separar, agendar o reservar un aula específica.")
    public String reservarAulaTool(
            @ToolParam(description = "Nombre del Aula a reservar (obligatorio) Ej: B101- Digital o B101") String nombreAula,
            @ToolParam(description = "Fecha de la reserva en formato yyyy-MM-dd") String fecha,
            @ToolParam(description = "Hora de inicio en formato HH:mm") String horaInicio,
            @ToolParam(description = "Hora de fin en formato HH:mm") String horaFin,
            @ToolParam(description = "Motivo o título de la reserva. Ej: Reunión de proyecto, Clase de refuerzo, Examen parcial") String motivo
    ) {
        System.out.println("=== [reservarAulaTool] INICIO ===");
        System.out.println("Aula: " + nombreAula + " | Fecha: " + fecha
                + " | De: " + horaInicio + " a: " + horaFin + " | Motivo: " + motivo);

        // ── 1. Resolver codigoAula desde el nombre y obtener metadatos ───────
        Long codigoAula = null;
        AulaDTO aulaEncontrada = null;
        try {
            ResponseAulaDTO response = aulaClient.buscarAulasPorNombre(nombreAula);
            if (response == null || response.aulas() == null || response.aulas().isEmpty()) {
                return "No encontré ninguna aula con el nombre '" + nombreAula + "'. Verifica el nombre e intenta de nuevo.";
            }
            aulaEncontrada = response.aulas().get(0);
            codigoAula = aulaEncontrada.codigoAula();
            System.out.println("codigoAula resuelto: " + codigoAula);
        } catch (FeignException e) {
            System.err.println("[reservarAulaTool] Error buscando aula: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[reservarAulaTool] Error inesperado buscando aula: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }

        // ── 2. Extraer identidad del usuario desde el JWT ────────────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "No se pudo identificar tu sesión. Por favor inicia sesión e intenta de nuevo.";
        }

        Long idSolicitante;
        try {
            idSolicitante = Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return "No se pudo determinar tu ID de usuario desde el token JWT.";
        }

        String rolStr = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DOCENTE");

        System.out.println("ID Solicitante: " + idSolicitante + " | Rol: " + rolStr);

        // ── 3. Validación proactiva de rol ESTUDIANTE ────────────────────────
        if ("ESTUDIANTE".equalsIgnoreCase(rolStr)) {
            String codigoTipoAula = (aulaEncontrada != null && aulaEncontrada.tipoAula() != null)
                    ? aulaEncontrada.tipoAula().codigoTipoAula() : null;
            if (!"78".equals(codigoTipoAula) && !"79".equals(codigoTipoAula)) {
                return "Como estudiante, solo puedes reservar aulas interactivas (tipo 78) o audiovisuales (tipo 79). " +
                        "El aula '" + nombreAula + "' es de tipo " +
                        (codigoTipoAula != null ? codigoTipoAula : "desconocido") +
                        ", por lo que no está permitida para tu rol.";
            }
        }

        // ── 4. Validar disponibilidad real (BD interna + SIGA) ───────────────
        try {
            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);
            if (ocupadas != null && ocupadas.contains(codigoAula)) {
                return "Lo siento, el aula '" + nombreAula + "' ya está ocupada el " + fecha
                        + " de " + horaInicio + " a " + horaFin
                        + " (reserva existente en AulaSmart o clase programada en el sistema SIGA).";
            }
        } catch (FeignException e) {
            System.err.println("[reservarAulaTool] Error verificando disponibilidad: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[reservarAulaTool] Error inesperado verificando disponibilidad: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }

        // ── 5. Construir payload y llamar al reserva-service ─────────────────
        try {
            String horaInicioISO = fecha + "T" + horaInicio + ":00";
            String horaFinISO = fecha + "T" + horaFin + ":00";

            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("codigoAula", codigoAula);
            payload.put("horaInicio", horaInicioISO);
            payload.put("horaFin", horaFinISO);
            payload.put("estado", "CONFIRMADA");
            payload.put("idSolicitante", idSolicitante);
            payload.put("rolSolicitante", rolStr);
            payload.put("titulo", motivo);

            System.out.println("[reservarAulaTool] Payload: " + payload);
            Map<String, Object> respuesta = reservaClient.crearReserva(payload);

            // Determinar estado final de la reserva
            String estadoFinal = "CONFIRMADA";
            Object reservaCreada = respuesta != null ? respuesta.get("reserva") : null;
            if (reservaCreada instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> reservaMap = (Map<String, Object>) reservaCreada;
                Object estadoObj = reservaMap.get("estado");
                if (estadoObj != null) {
                    estadoFinal = estadoObj.toString();
                }
            }

            if ("PENDIENTE".equalsIgnoreCase(estadoFinal)) {
                return "Tu reserva para el aula '" + nombreAula
                        + "' fue creada y quedó **PENDIENTE de autorización**. "
                        + "Motivo: " + motivo + ". "
                        + "El administrador debe aprobarla. Te notificaremos cuando sea revisada. "
                        + "Fecha: " + fecha + " de " + horaInicio + " a " + horaFin + ".";
            } else {
                return "¡Tu reserva fue **CONFIRMADA** exitosamente! El aula '" + nombreAula
                        + "' ha sido reservada el " + fecha
                        + " de " + horaInicio + " a " + horaFin
                        + " para: " + motivo + ".";
            }

        } catch (FeignException e) {
            System.err.println("[reservarAulaTool] Error al crear reserva: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[reservarAulaTool] Error inesperado al crear reserva: " + e.getMessage());
            return MSG_ERROR_GENERICO;
        }
    }

    @Tool(name = "consultarHorariosAula", description = "Consulta los horarios ocupados y disponibles de un aula específica en una fecha dada.")
    public String consultarHorariosAula(
            @ToolParam(description = "Nombre del aula. Ej: AULA 101 o B104") String nombreAula,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd. Ej: 2026-05-14") String fecha
    ) {
        System.out.println("=== [consultarHorariosAula] INICIO ===");
        System.out.println("Aula: " + nombreAula + " | Fecha: " + fecha);

        try {
            // 1. Resolver codigoAula
            ResponseAulaDTO response = aulaClient.buscarAulasPorNombre(nombreAula);
            if (response == null || response.aulas() == null || response.aulas().isEmpty()) {
                return "No encontré ninguna aula con el nombre '" + nombreAula + "'. Verifica el nombre e intenta de nuevo.";
            }
            Long codigoAula = response.aulas().get(0).codigoAula();
            String nombreReal = response.aulas().get(0).nombreAula();
            System.out.println("Aula resuelta: " + nombreReal + " (codigo=" + codigoAula + ")");

            // 2. Obtener reservas del aula (AulaSmart + SIGA)
            Map<String, Object> respuestaReservas = reservaClient.obtenerReservasPorAula(codigoAula);
            List<Map<String, Object>> reservas = extraerReservas(respuestaReservas);

            // 3. Filtrar reservas por fecha
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

            // 4. Construir respuesta
            StringBuilder sb = new StringBuilder();
            sb.append("Horarios del aula **").append(nombreReal).append("** el **").append(fecha).append("**:\n\n");

            if (ocupados.isEmpty()) {
                sb.append("✅ El aula está completamente libre ese día.\n");
            } else {
                sb.append("❌ **Horarios OCUPADOS:**\n");
                for (String[] r : ocupados) {
                    sb.append("  • ").append(r[0]).append(" - ").append(r[1]).append("\n");
                }
            }

            // 5. Sugerir horarios libres (7am - 7pm en bloques de 2 horas)
            sb.append("\n💡 **Horarios sugeridos (libres):**\n");
            int[][] bloques = {{7,9},{9,11},{11,13},{13,15},{15,17},{17,19}};
            for (int[] bloque : bloques) {
                String hInicio = String.format("%02d:00", bloque[0]);
                String hFin = String.format("%02d:00", bloque[1]);
                if (!estaOcupado(hInicio, hFin, ocupados)) {
                    sb.append("  • ").append(hInicio).append(" - ").append(hFin).append(" ✅\n");
                } else {
                    sb.append("  • ").append(hInicio).append(" - ").append(hFin).append(" ❌ Ocupado\n");
                }
            }

            sb.append("\nSi quieres reservar uno de estos horarios, dímelo y lo agendo.");
            return sb.toString();

        } catch (FeignException e) {
            System.err.println("[consultarHorariosAula] Error de comunicación: " + e.getMessage());
            return MSG_ERROR_RED;
        } catch (Exception e) {
            System.err.println("[consultarHorariosAula] Error inesperado: " + e.getMessage());
            e.printStackTrace();
            return MSG_ERROR_GENERICO;
        }
    }

    // ── Helpers para consultarHorariosAula ─────────────────────────────────

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
            // Puede venir como "2026-05-14T08:00:00" o "2026-05-14 08:00:00"
            String limpio = valor.replace(" ", "T");
            if (limpio.contains(".")) {
                limpio = limpio.substring(0, limpio.indexOf('.'));
            }
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
            // Solapamiento: A.inicio < B.fin && A.fin > B.inicio
            if (tInicio.isBefore(oFin) && tFin.isAfter(oInicio)) {
                return true;
            }
        }
        return false;
    }
}
