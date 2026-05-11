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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /**
     * Retorna los codigosAula ocupados en un rango de tiempo, combinando
     * reservas internas (BD) y reservas externas (SIGA) con detección de solapamiento.
     *
     * Para las reservas SIGA, itera sobre todas las aulas conocidas en la BD
     * que tengan código SIGA asignado, consulta la API por fecha y verifica si
     * se solapan con el rango solicitado.
     *
     * @param horaInicio inicio del rango a consultar (inclusive)
     * @param horaFin    fin del rango a consultar (exclusive)
     * @return lista de codigosAula (propios de AulaSmart) ocupados en ese rango
     */
    public List<Long> obtenerAulasOcupadasEnRangoConSiga(LocalDateTime horaInicio, LocalDateTime horaFin) {
        Set<Long> ocupadas = new HashSet<>();

        // 1. Aulas ocupadas en la BD interna de AulaSmart
        List<Long> ocupadasBD = reservaRepository.findAulasOcupadasEnRango(horaInicio, horaFin);
        ocupadas.addAll(ocupadasBD);

        // 2. Aulas ocupadas en SIGA para la misma fecha
        LocalDate fecha = horaInicio.toLocalDate();

        // Todos los codigosAula registrados en el aula-service
        List<Long> todosLosCodigos;
        try {
            todosLosCodigos = aulaClient.listarCodigosAula();
        } catch (Exception e) {
            System.err.println("[AgregadorReservasService] No se pudo obtener la lista de aulas: " + e.getMessage());
            return new ArrayList<>(ocupadas); // fallback: solo BD interna
        }

        // Para cada aula, consultamos el SIGA y verificamos solapamiento
        // Usamos parallelStream para realizar las peticiones HTTP de forma concurrente y mejorar el tiempo de respuesta
        List<Long> ocupadasSiga = todosLosCodigos.parallelStream()
                .filter(codigoAula -> !ocupadas.contains(codigoAula))
                .filter(codigoAula -> {
                    try {
                        // El codigoAula ES el código SIGA (se usa el mismo valor en la API del SIGA)
                        List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAulaYFecha(codigoAula, fecha);
                        return reservasSiga.stream().anyMatch(r -> seSolapa(r, horaInicio, horaFin));
                    } catch (Exception e) {
                        System.err.println("[AgregadorReservasService] Error consultando SIGA para aula " + codigoAula + ": " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());

        ocupadas.addAll(ocupadasSiga);


        return new ArrayList<>(ocupadas);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Verifica si una reserva SIGA se solapa con el rango [inicio, fin). */
    private boolean seSolapa(SigaReservaDTO siga, LocalDateTime inicio, LocalDateTime fin) {
        try {
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime sigaInicio = LocalDateTime.parse(siga.getStart_dt(), fmt);
            LocalDateTime sigaFin    = LocalDateTime.parse(siga.getEnd_dt(),   fmt);
            // Solapamiento estándar: A.inicio < B.fin && A.fin > B.inicio
            return sigaInicio.isBefore(fin) && sigaFin.isAfter(inicio);
        } catch (Exception e) {
            return false; // si no se puede parsear, ignoramos esa reserva
        }
    }
}