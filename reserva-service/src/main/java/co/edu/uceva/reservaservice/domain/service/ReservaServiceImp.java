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
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor //esto inyecta el repo actomáticamente
public class ReservaServiceImp implements IReservaService{

    //inyectar la dependencia del repositorio
    private final IReservaRepository reservaRepository;

    @Override
    @Transactional
    public Reserva addReserva(Reserva reserva) {
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

    @Override
    @Transactional (readOnly = true)
    public Optional<Reserva> findReservaById(Long id) {
        return reservaRepository.findById(id);
    }

    @Override
    @Transactional
    public Reserva updateReserva(Reserva reserva) {
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
