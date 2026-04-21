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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReservaDTO traducir(SigaReservaDTO sigaReserva) {
        ReservaDTO dto = new ReservaDTO();
        
        dto.setIdReserva("SIGA-" + sigaReserva.getCod_resv());
        dto.setCodigoAula(sigaReserva.getCod_aula());
        
        String fechaInicioStr = sigaReserva.getFec_res() + " " + sigaReserva.getH_ini();
        dto.setHoraInicio(LocalDateTime.parse(fechaInicioStr, DATE_FORMAT));
        
        String fechaFinStr = sigaReserva.getFec_res() + " " + sigaReserva.getH_fin();
        dto.setHoraFin(LocalDateTime.parse(fechaFinStr, DATE_FORMAT));
        
        dto.setEstado(sigaReserva.getStatus().equals("A") ? EstadosReserva.CONFIRMADA : EstadosReserva.CANCELADA);
        
        // Datos simulados para SIGA ya que no provienen de la app
        dto.setIdSolicitante(0L); // 0 indica sistema/oficial
        dto.setRolSolicitante(RolUsuario.ADMINISTRATIVO); 
        dto.setCodigoPrograma(sigaReserva.getPrograma());
        dto.setGrupo(sigaReserva.getGrupo());
        
        dto.setOrigen("SIGA");
        
        return dto;
    }
}
