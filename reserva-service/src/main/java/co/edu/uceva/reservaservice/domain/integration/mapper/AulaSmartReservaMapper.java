package co.edu.uceva.reservaservice.domain.integration.mapper;

import co.edu.uceva.reservaservice.domain.integration.dto.ReservaDTO;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.stereotype.Component;

@Component
public class AulaSmartReservaMapper {

    public ReservaDTO traducir(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        
        dto.setIdReserva(reserva.getIdReserva().toString());
        dto.setCodigoAula(reserva.getCodigoAula());
        dto.setHoraInicio(reserva.getHoraInicio());
        dto.setHoraFin(reserva.getHoraFin());
        dto.setEstado(reserva.getEstado());
        dto.setIdSolicitante(reserva.getIdSolicitante());
        dto.setRolSolicitante(reserva.getRolSolicitante());
        dto.setCodigoPrograma(reserva.getCodigoPrograma());
        dto.setGrupo(reserva.getGrupo());
        dto.setOrigen("AULASMART");
        
        return dto;
    }
}
