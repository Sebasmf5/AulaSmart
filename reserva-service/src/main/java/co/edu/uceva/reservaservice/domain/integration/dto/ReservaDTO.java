package co.edu.uceva.reservaservice.domain.integration.dto;

import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.RolUsuario;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaDTO {
    private String idReserva; 
    private Long codigoAula;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private EstadosReserva estado;
    private Long idSolicitante; 
    private RolUsuario rolSolicitante;
    private String codigoPrograma;
    private String grupo;
    private String origen; // "AULASMART" o "SIGA"
}
