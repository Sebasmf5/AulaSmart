package co.edu.uceva.reservaservice.domain.service;

import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.repository.IReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor //esto inyecta el repo actomáticamente
public class ReservaServiceImp implements IReservaService{

    //inyectar la dependencia del repositorio
    private final IReservaRepository reservaRepository;

    @Override
    @Transactional
    public Reserva addReserva(Reserva reserva) {
        // Usamos la consulta SQL para verificar si el aula está libre o se cruza
        boolean estaOcupado = reservaRepository.existeCruceDeHorarios(
                reserva.getCodigoAula(),
                reserva.getHoraInicio(),
                reserva.getHoraFin()
        );
        if(estaOcupado){
            throw  new RuntimeException("Reserva ya ocupado");
        }
        reserva.setEstado(EstadosReserva.CONFIRMADA);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional (readOnly = true)
    public Optional<Reserva> findReservaById(Long id) {
        return reservaRepository.findById(id);
    }

    @Override
    @Transactional
    public Reserva updateReserva(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public void deleteReserva(Reserva reserva) {
        reservaRepository.delete(reserva);
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
