package co.edu.uceva.reservaservice.domain.service;


import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interface que define los métodos que se pueden realizar sobre la entidad Reserva
 */
public interface IReservaService {

    //create
    Reserva addReserva(Reserva reserva);
    //read
    Optional<Reserva> findReservaById(Long id);
    //update
    Reserva updateReserva(Reserva reserva);
    //delete
    void deleteReserva(Reserva reserva);
    List<Reserva> findAll();
    Page<Reserva> findAll(Pageable pageable);
}
