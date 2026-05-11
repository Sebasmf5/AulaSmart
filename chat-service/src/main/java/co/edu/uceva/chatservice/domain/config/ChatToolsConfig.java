package co.edu.uceva.chatservice.domain.config;

import co.edu.uceva.chatservice.domain.FeignClients.IAulaServiceClient;
import co.edu.uceva.chatservice.domain.FeignClients.IReservaServiceClient;
import co.edu.uceva.chatservice.domain.dto.AulaDTO;
import co.edu.uceva.chatservice.domain.dto.BloqueDTO;
import co.edu.uceva.chatservice.domain.dto.ResponseAulaDTO;
import co.edu.uceva.chatservice.domain.model.ReservarAulaArgs;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class    ChatToolsConfig {

    private final IAulaServiceClient aulaClient;
    private final IReservaServiceClient reservaClient;

    public ChatToolsConfig(IAulaServiceClient aulaClient, IReservaServiceClient reservaClient) {
        this.aulaClient = aulaClient;
        this.reservaClient = reservaClient;
    }

    @Tool(name = "consultarPorBloque", description = "Consulta aulas disponibles en un bloque específico en una fecha y horario.")
    public String consultarPorBloque(
            @ToolParam(description = "Nombre del bloque. Ej: BLOQUE B - AVELLANOS") String bloque,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd. Ej: 2026-05-15 o 15 de mayo") String fecha,
            @ToolParam(description = "Hora de inicio en formato HH:mm. Ej: 08:00") String horaInicio,
            @ToolParam(description = "Hora de fin en formato HH:mm. Ej: 10:00") String horaFin
    ) {
        System.out.println("=== [consultarPorBloque] INICIO ===");
        System.out.println("Parametros: bloque=" + bloque + ", fecha=" + fecha + ", inicio=" + horaInicio + ", fin=" + horaFin);
        try {
            System.out.println("1. Llamando a aulaClient.buscarBloque");
            Map<String, BloqueDTO> responseMap = aulaClient.buscarBloque(bloque);
            BloqueDTO bloqueDTO = responseMap != null ? responseMap.get("bloque") : null;
            if (bloqueDTO == null || bloqueDTO.id() == null) {
                System.out.println("-> Bloque no encontrado");
                return "No encontré ningún bloque con el nombre '" + bloque + "'.";
            }
            System.out.println("2. Bloque encontrado: " + bloqueDTO.id());
            
            System.out.println("3. Llamando a aulaClient.listarAulasPorBloque");
            ResponseAulaDTO responseAula = aulaClient.listarAulasPorBloque(bloqueDTO.id());
            if (responseAula == null || responseAula.aulas() == null || responseAula.aulas().isEmpty()) {
                System.out.println("-> No hay aulas registradas");
                return "El bloque " + bloqueDTO.nombre() + " no tiene aulas registradas.";
            }
            System.out.println("4. Aulas en bloque: " + responseAula.aulas().size());
            
            System.out.println("5. Llamando a reservaClient.obtenerAulasOcupadas");
            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);
            System.out.println("6. Aulas ocupadas devueltas: " + (ocupadas != null ? ocupadas.size() : "null"));
            
            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> !ocupadas.contains(a.codigoAula()))
                    .collect(Collectors.toList());
            if (disponibles.isEmpty()) {
                return "No hay aulas disponibles en el bloque " + bloqueDTO.nombre() + " el " + fecha + " de " + horaInicio + " a " + horaFin + ".";
            }
            StringBuilder sb = new StringBuilder("Aulas disponibles en ").append(bloqueDTO.nombre())
                    .append(" el ").append(fecha).append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula()).append(" (Capacidad: ").append(a.capacidad()).append(")\n"));
            System.out.println("7. Retornando " + disponibles.size() + " aulas disponibles");
            return sb.toString();
        } catch (Exception e) {
            System.err.println("=== ERROR EN consultarPorBloque ===");
            e.printStackTrace();
            return "Error al consultar disponibilidad: " + e.getMessage();
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
                return "No encontré aulas del tipo '" + tipoAula + "'.";
            }
            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);
            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(a -> !ocupadas.contains(a.codigoAula()))
                    .collect(Collectors.toList());
            if (disponibles.isEmpty()) {
                return "No hay aulas de tipo " + tipoAula + " disponibles el " + fecha + " de " + horaInicio + " a " + horaFin + ".";
            }
            StringBuilder sb = new StringBuilder("Aulas de tipo ").append(tipoAula)
                    .append(" disponibles el ").append(fecha).append(" de ").append(horaInicio).append(" a ").append(horaFin).append(":\n");
            disponibles.forEach(a -> sb.append("- ").append(a.nombreAula())
                    .append(" (Bloque: ").append(a.bloque().nombre()).append(", Capacidad: ").append(a.capacidad()).append(")\n"));
            return sb.toString();
        } catch (Exception e) {
            return "Error al consultar disponibilidad: " + e.getMessage();
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
                return "No encontré ninguna aula con el nombre '" + nombreAula + "'.";
            }

            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(fecha, horaInicio, horaFin);

            List<AulaDTO> disponibles = responseAula.aulas().stream()
                    .filter(aula -> !ocupadas.contains(aula.codigoAula()))
                    .collect(Collectors.toList());

            if (disponibles.isEmpty()) {
                return "Lo siento, el aula '" + nombreAula + "' está ocupada el " + fecha + " de " + horaInicio + " a " + horaFin + ".";
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
        } catch (Exception e) {
            return "Error al consultar el aula: " + e.getMessage();
        }
    }

    @Tool(name = "reservarAulaTool", description = "Úsalo SIEMPRE que el usuario dé una orden directa de separar, agendar o reservar un aula específica.")
    public String reservarAulaTool(ReservarAulaArgs args) {
        System.out.println("=== [reservarAulaTool] INICIO ===");
        System.out.println("Aula: " + args.nombreAula() + " | Fecha: " + args.fecha()
                + " | De: " + args.horaInicio() + " a: " + args.horaFin());

        // ── 1. Resolver codigoAula desde el nombre ─────────────────────────
        Long codigoAula = args.codigoAula();
        if (codigoAula == null) {
            try {
                ResponseAulaDTO response = aulaClient.buscarAulasPorNombre(args.nombreAula());
                if (response == null || response.aulas() == null || response.aulas().isEmpty()) {
                    return "No encontré ninguna aula con el nombre '" + args.nombreAula() + "'. Verifica el nombre e intenta de nuevo.";
                }
                codigoAula = response.aulas().get(0).codigoAula();
                System.out.println("codigoAula resuelto: " + codigoAula);
            } catch (Exception e) {
                return "Error al buscar el aula '" + args.nombreAula() + "': " + e.getMessage();
            }
        }

        // ── 2. Validar disponibilidad real (BD interna + SIGA) ─────────────
        try {
            List<Long> ocupadas = reservaClient.obtenerAulasOcupadas(args.fecha(), args.horaInicio(), args.horaFin());
            if (ocupadas.contains(codigoAula)) {
                return "Lo siento, el aula '" + args.nombreAula() + "' ya está ocupada el " + args.fecha()
                        + " de " + args.horaInicio() + " a " + args.horaFin()
                        + " (reserva existente en AulaSmart o clase programada en el sistema SIGA).";
            }
        } catch (Exception e) {
            System.err.println("[reservarAulaTool] Error verificando disponibilidad: " + e.getMessage());
            return "No pude verificar la disponibilidad del aula en este momento. Inténtalo de nuevo más tarde.";
        }

        // ── 3. Extraer identidad del usuario desde el JWT (Spring Security) ─
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "No se pudo identificar tu sesión. Por favor inicia sesión e intenta de nuevo.";
        }

        Long idSolicitante;
        try {
            idSolicitante = Long.valueOf(auth.getName()); // el subject del JWT es el ID del usuario
        } catch (NumberFormatException e) {
            return "No se pudo determinar tu ID de usuario desde el token JWT.";
        }

        String rolStr = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DOCENTE");

        System.out.println("ID Solicitante: " + idSolicitante + " | Rol: " + rolStr);

        // ── 4. Construir payload y llamar al reserva-service ──────────────
        try {
            // Construir LocalDateTime: "2026-05-15T08:00:00"
            String horaInicioISO = args.fecha() + "T" + args.horaInicio() + ":00";
            String horaFinISO    = args.fecha() + "T" + args.horaFin()    + ":00";

            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("codigoAula",   codigoAula);
            payload.put("horaInicio",   horaInicioISO);
            payload.put("horaFin",      horaFinISO);
            payload.put("estado",       "CONFIRMADA");
            payload.put("idSolicitante", idSolicitante);
            payload.put("rolSolicitante", rolStr);
            payload.put("titulo",       "Reserva via AulaSmart Chat");

            System.out.println("[reservarAulaTool] Payload: " + payload);
            Map<String, Object> respuesta = reservaClient.crearReserva(payload);

            Object reservaCreada = respuesta.get("reserva");
            return "✅ ¡Reserva creada con éxito! El aula '" + args.nombreAula()
                    + "' ha sido reservada el " + args.fecha()
                    + " de " + args.horaInicio() + " a " + args.horaFin() + "."
                    + (reservaCreada != null ? " Detalles: " + reservaCreada : "");

        } catch (Exception e) {
            System.err.println("[reservarAulaTool] Error al crear reserva: " + e.getMessage());
            return "Ocurrió un error al intentar crear la reserva: " + e.getMessage()
                    + ". Por favor intenta de nuevo o contacta al administrador.";
        }
    }
}
