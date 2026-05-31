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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgregadorReservasService {

    private final IReservaRepository reservaRepository;
    private final ISigaClient sigaClient;
    private final IAulaClient aulaClient;
    private final SigaReservaMapper sigaMapper;
    private final AulaSmartReservaMapper aulaSmartMapper;

    /**
     * Obtiene todas las reservas de un aula (AulaSmart + SIGA) dado su ID interno.
     */
    public List<ReservaDTO> obtenerTodasLasReservas(Long aulaId) {
        // 1. Obtener de AulaSmart (por aulaId)
        List<Reserva> reservasInternas = reservaRepository.findByAulaId(aulaId);
        List<ReservaDTO> listaAulaSmart = reservasInternas.stream()
                .map(aulaSmartMapper::traducir)
                .collect(Collectors.toList());

        // 2. Obtener de SIGA (necesitamos el codigoAula/pasaporte SIGA)
        List<ReservaDTO> listaSiga = new java.util.ArrayList<>();
        try {
            Long codigoSiga = aulaClient.getCodigoSigaDeAula(aulaId);
            System.out.println("[Agregador] codigoSiga para aulaId=" + aulaId + ": " + codigoSiga);
            if (codigoSiga != null) {
                List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAula(codigoSiga);
                listaSiga = reservasSiga.stream()
                        .map(sigaMapper::traducir)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("[AgregadorReservasService] Error consultando SIGA para aulaId " + aulaId + ": " + e.getMessage());
            // Si falla la consulta a SIGA, retornamos solo las reservas internas
        }

        // 3. Agregar y retornar
        List<ReservaDTO> todasLasReservas = new ArrayList<>();
        todasLasReservas.addAll(listaAulaSmart);
        todasLasReservas.addAll(listaSiga);

        return todasLasReservas;
    }

    /**
     * Obtiene las reservas SIGA para un aula en una fecha específica.
     * Recibe el aulaId (PK) y consulta SIGA usando el codigoAula asociado.
     */
    public List<ReservaDTO> obtenerReservasSigaPorAulaId(Long aulaId, LocalDate fecha) {
        try {
            Long codigoSiga = aulaClient.getCodigoSigaDeAula(aulaId);
            if (codigoSiga == null) {
                return new java.util.ArrayList<>();
            }
            List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAulaYFecha(codigoSiga, fecha);
            return reservasSiga.stream()
                    .map(sigaMapper::traducir)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[AgregadorReservasService] Error consultando SIGA por fecha para aulaId " + aulaId + ": " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Retorna los aulaId ocupados en un rango de tiempo, combinando
     * reservas internas (BD) y reservas externas (SIGA) con detección de solapamiento.
     *
     * @param horaInicio inicio del rango a consultar (inclusive)
     * @param horaFin    fin del rango a consultar (exclusive)
     * @return lista de aulaId (PK) ocupados en ese rango
     */
    public List<Long> obtenerAulasOcupadasEnRangoConSiga(LocalDateTime horaInicio, LocalDateTime horaFin) {
        Set<Long> ocupadas = new HashSet<>();

        // 1. Aulas ocupadas en la BD interna de AulaSmart (todos los tipos de aula)
        List<Long> ocupadasBD = reservaRepository.findAulasOcupadasEnRango(horaInicio, horaFin);
        ocupadas.addAll(ocupadasBD);

        // 2. Aulas ocupadas en SIGA para la misma fecha
        LocalDate fecha = horaInicio.toLocalDate();

        List<Map<String, Object>> aulasSincronizadas;
        try {
            aulasSincronizadas = aulaClient.listarAulasSincronizadasSiga();
        } catch (Exception e) {
            System.err.println("[AgregadorReservasService] No se pudo obtener la lista de aulas sincronizadas con SIGA: " + e.getMessage());
            return new ArrayList<>(ocupadas); // fallback: solo BD interna
        }

        if (aulasSincronizadas == null || aulasSincronizadas.isEmpty()) {
            return new ArrayList<>(ocupadas);
        }

        // Para cada aula sincronizada con SIGA, consultamos la API externa con su codigoAula
        // y si esta ocupada, agregamos su aulaId a la lista.
        // Pool de 16 hilos con timeout 1500ms por llamada SIGA.
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        for (Map<String, Object> map : aulasSincronizadas) {
            Object aulaIdObj = map.get("id");
            if (aulaIdObj == null || ocupadas.contains(((Number) aulaIdObj).longValue())) continue;
            Object codigoAulaObj = map.get("codigoAula");
            if (codigoAulaObj == null) continue;
            Long codigoAula = ((Number) codigoAulaObj).longValue();
            Long aulaId = ((Number) aulaIdObj).longValue();
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    List<SigaReservaDTO> reservasSiga = sigaClient.obtenerReservasPorAulaYFecha(codigoAula, fecha);
                    if (reservasSiga.stream().anyMatch(r -> seSolapa(r, horaInicio, horaFin))) {
                        return aulaId;
                    }
                } catch (Exception e) {
                    return aulaId; // si SIGA falla, asumir ocupada por seguridad
                }
                return null;
            }, executor));
        }
        for (CompletableFuture<Long> f : futures) {
            try {
                Long id = f.get(2000, TimeUnit.MILLISECONDS);
                if (id != null) ocupadas.add(id);
            } catch (Exception ignored) {
            }
        }
        executor.shutdown();

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
