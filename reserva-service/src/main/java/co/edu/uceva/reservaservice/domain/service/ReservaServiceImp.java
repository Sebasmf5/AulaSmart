package co.edu.uceva.reservaservice.domain.service;

import co.edu.uceva.reservaservice.domain.excepcion.ReservaModificadaException;
import co.edu.uceva.reservaservice.domain.excepcion.ReservaSolapadaException;
import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.repository.IReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import co.edu.uceva.reservaservice.domain.integration.service.AgregadorReservasService;
import co.edu.uceva.reservaservice.domain.integration.dto.ReservaDTO;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor //esto inyecta el repo actomáticamente
public class ReservaServiceImp implements IReservaService{

    //inyectar la dependencia del repositorio
    private final IReservaRepository reservaRepository;
    private final AgregadorReservasService agregadorReservasService;

    @Override
    @Transactional
    public Reserva addReserva(Reserva reserva) {
        // Validar solapamiento externo (SIGA) antes de intentar guardar
        List<ReservaDTO> reservasSiga = agregadorReservasService.obtenerReservasSigaPorFecha(
            reserva.getCodigoAula(),
            reserva.getHoraInicio().toLocalDate()
        );

        boolean solapamientoExterno = reservasSiga.stream().anyMatch(reservaSiga -> 
            fechasSeSolapan(
                reserva.getHoraInicio(), reserva.getHoraFin(),
                reservaSiga.getHoraInicio(), reservaSiga.getHoraFin()
            )
        );

        if (solapamientoExterno) {
            throw new ReservaSolapadaException();
        }

        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            if (message != null && message.contains("no_solapamiento_reservas")) {
                throw new ReservaSolapadaException();
            }
            throw e;
        }
    }

    private boolean fechasSeSolapan(LocalDateTime inicio1, LocalDateTime fin1, LocalDateTime inicio2, LocalDateTime fin2) {
        return inicio1.isBefore(fin2) && fin1.isAfter(inicio2);
    }

    @Override
    @Transactional (readOnly = true)
    public Optional<Reserva> findReservaById(Long id) {
        return reservaRepository.findById(id);
    }

    @Override
    @Transactional
    public Reserva updateReserva(Reserva reserva) {
        // Validar solapamiento externo (SIGA) antes de intentar actualizar
        List<ReservaDTO> reservasSiga = agregadorReservasService.obtenerReservasSigaPorFecha(
            reserva.getCodigoAula(),
            reserva.getHoraInicio().toLocalDate()
        );

        boolean solapamientoExterno = reservasSiga.stream().anyMatch(reservaSiga -> 
            fechasSeSolapan(
                reserva.getHoraInicio(), reserva.getHoraFin(),
                reservaSiga.getHoraInicio(), reservaSiga.getHoraFin()
            )
        );

        if (solapamientoExterno) {
            throw new ReservaSolapadaException();
        }

        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            if (message != null && message.contains("no_solapamiento_reservas")) {
                throw new ReservaSolapadaException();
            }
            throw e;
        }catch (ObjectOptimisticLockingFailureException e) {
            // Alguien más modificó esta reserva antes que tú
            throw new ReservaModificadaException(reserva.getIdReserva());
        }
    }

    @Override
    @Transactional
    public void deleteReserva(Reserva reserva) {
        reserva.setEstado(EstadosReserva.CANCELADA);
        reservaRepository.save(reserva);
    }

    @Override
    @Transactional (readOnly = true)
    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }

    @Override
    @Transactional (readOnly = true)
    public Page<Reserva> findAll(Pageable pageable) {
        return reservaRepository.findAll(pageable);
    }
}
