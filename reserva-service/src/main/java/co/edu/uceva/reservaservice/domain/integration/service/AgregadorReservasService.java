package co.edu.uceva.reservaservice.domain.integration.service;

import co.edu.uceva.reservaservice.domain.integration.client.ISigaClient;
import co.edu.uceva.reservaservice.domain.integration.dto.ReservaDTO;
import co.edu.uceva.reservaservice.domain.service.IAulaClient;
import co.edu.uceva.reservaservice.domain.integration.dto.SigaReservaDTO;
import co.edu.uceva.reservaservice.domain.integration.mapper.AulaSmartReservaMapper;
import co.edu.uceva.reservaservice.domain.integration.mapper.SigaReservaMapper;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.repository.IReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgregadorReservasService {

    private final IReservaRepository reservaRepository;
    private final ISigaClient sigaClient;
    private final IAulaClient aulaClient;
    private final SigaReservaMapper sigaMapper;
    private final AulaSmartReservaMapper aulaSmartMapper;

    public List<ReservaDTO> obtenerTodasLasReservas(Long codigoAula) {
        // 1. Obtener de AulaSmart
        List<Reserva> reservasInternas = reservaRepository.findByCodigoAula(codigoAula);
        List<ReservaDTO> listaAulaSmart = reservasInternas.stream()
                .map(aulaSmartMapper::traducir)
                .collect(Collectors.toList());

        // 2. Obtener de SIGA
        Integer codigoSiga = aulaClient.getSigaDeAula(codigoAula);
        System.out.println(codigoSiga);
        List<ReservaDTO> listaSiga = new java.util.ArrayList<>();
        if (codigoSiga != null) {
            List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAula(codigoSiga.longValue());
            listaSiga = reservasSiga.stream()
                    .map(sigaMapper::traducir)
                    .collect(Collectors.toList());
        }

        // 3. Agregar y retornar
        List<ReservaDTO> todasLasReservas = new ArrayList<>();
        todasLasReservas.addAll(listaAulaSmart);
        todasLasReservas.addAll(listaSiga);

        return todasLasReservas;
    }

    public List<ReservaDTO> obtenerReservasSigaPorFecha(Long codigoAula, LocalDate fecha) {
        Integer codigoSiga = aulaClient.getSigaDeAula(codigoAula);
        if (codigoSiga == null) {
            return new java.util.ArrayList<>();
        }
        List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAulaYFecha(codigoSiga.longValue(), fecha);
        return reservasSiga.stream()
                .map(sigaMapper::traducir)
                .collect(Collectors.toList());
    }
}