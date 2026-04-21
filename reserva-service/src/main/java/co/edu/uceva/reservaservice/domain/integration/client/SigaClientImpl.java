package co.edu.uceva.reservaservice.domain.integration.client;

import co.edu.uceva.reservaservice.domain.integration.dto.SigaReservaDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SigaClientMock implements ISigaClient {

    @Override
    public List<SigaReservaDTO> obtenerReservasPorAula(Long codigoAula) {
        // Simulamos algunas reservas que siempre están ahí
        List<SigaReservaDTO> mockReservas = new ArrayList<>();
        
        SigaReservaDTO r1 = new SigaReservaDTO();
        r1.setCod_resv(1001L);
        r1.setCod_aula(codigoAula);
        r1.setFec_res(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        r1.setH_ini("08:00");
        r1.setH_fin("10:00");
        r1.setStatus("A");
        r1.setPrograma("Ingeniería de Sistemas");
        r1.setGrupo("A");
        
        mockReservas.add(r1);
        return mockReservas;
    }

    @Override
    public List<SigaReservaDTO> obtenerReservasPorAulaYFecha(Long codigoAula, LocalDate fecha) {
        // En una implementación real se filtra por fecha
        return obtenerReservasPorAula(codigoAula);
    }
}
