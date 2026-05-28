package co.edu.uceva.reservaservice.domain.integration.mapper;

import co.edu.uceva.reservaservice.domain.integration.dto.ReservaDTO;
import co.edu.uceva.reservaservice.domain.integration.dto.SigaReservaDTO;
import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.RolUsuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SigaReservaMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ReservaDTO traducir(SigaReservaDTO sigaReserva) {
        ReservaDTO dto = new ReservaDTO();
        
        dto.setIdReserva("SIGA-" + sigaReserva.getId_unico());
        dto.setAulaId(Long.valueOf(sigaReserva.getCode_location()));
        
        dto.setHoraInicio(LocalDateTime.parse(sigaReserva.getStart_dt(), DATE_FORMAT));
        dto.setHoraFin(LocalDateTime.parse(sigaReserva.getEnd_dt(), DATE_FORMAT));
        
        dto.setEstado(EstadosReserva.CONFIRMADA); // Clases institucionales confirmadas por defecto
        
        dto.setIdSolicitante(sigaReserva.getCodigo_docente_responsable()); 
        dto.setRolSolicitante(RolUsuario.DOCENTE); 
        // Procesamiento del título del evento para extraer la información agregada correctamente
        String rawTitle = sigaReserva.getEvt_title();
        String codigoPrograma = "No definido";
        String tituloLimpio = rawTitle;
        String grupo = "No definido";

        if (rawTitle != null && rawTitle.startsWith("Asignatura:")) {
            // Ejemplo: "Asignatura: 44110-441 - MÉTODOS... - Grp interno: 1 - Grp externo: 1 - Periodo: ENE/JUN 2026 PREGRADO"
            String[] parts = rawTitle.split(" - ");
            if (parts.length >= 2) {
                codigoPrograma = parts[0].replace("Asignatura:", "").trim();
                tituloLimpio = parts[1].trim();
                
                for (String part : parts) {
                    if (part.trim().startsWith("Grp interno:")) {
                        grupo = part.replace("Grp interno:", "").trim();
                        break; // Prioridad al grupo interno
                    } else if (part.trim().startsWith("Grp externo:")) {
                        grupo = part.replace("Grp externo:", "").trim();
                    }
                }
            }
        }

        dto.setCodigoPrograma(codigoPrograma);
        dto.setGrupo(grupo);
        dto.setTitulo(tituloLimpio);
        
        dto.setOrigen("SIGA");
        dto.setNombreUsuarioResponsable(sigaReserva.getNombre_docente_responsable());
        
        return dto;
    }
}
