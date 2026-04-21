package co.edu.uceva.reservaservice.domain.integration.client;

import co.edu.uceva.reservaservice.domain.integration.dto.SigaReservaDTO;
import java.time.LocalDate;
import java.util.List;

public interface ISigaClient {
    List<SigaReservaDTO> obtenerReservasPorAula(Long codigoAula);
    List<SigaReservaDTO> obtenerReservasPorAulaYFecha(Long codigoAula, LocalDate fecha);
}
