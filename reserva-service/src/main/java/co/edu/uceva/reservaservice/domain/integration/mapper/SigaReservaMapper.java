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
        dto.setCodigoAula(Long.valueOf(sigaReserva.getCode_location()));
        
        dto.setHoraInicio(LocalDateTime.parse(sigaReserva.getStart_dt(), DATE_FORMAT));
        dto.setHoraFin(LocalDateTime.parse(sigaReserva.getEnd_dt(), DATE_FORMAT));
        
        dto.setEstado(EstadosReserva.CONFIRMADA); // Clases institucionales confirmadas por defecto
        
        dto.setIdSolicitante(sigaReserva.getCodigo_docente_responsable()); 
        dto.setRolSolicitante(RolUsuario.DOCENTE); 
        dto.setCodigoPrograma(sigaReserva.getEvt_title()); // Mapear el título del evento como programa/clase
        dto.setGrupo(sigaReserva.getNombre_docente_responsable()); // Usar el nombre del docente en este campo
        
        dto.setOrigen("SIGA");
        
        return dto;
    }
}
