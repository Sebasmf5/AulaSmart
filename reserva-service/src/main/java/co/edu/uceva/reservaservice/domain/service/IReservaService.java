package co.edu.uceva.reservaservice.domain.service;


import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.data.domain.Page;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

/**
 * Interface que define los métodos que se pueden realizar sobre la entidad Reserva
 */
public interface IReservaService {

    //create
    Reserva addReserva(Reserva reserva);
    //read
    List<Reserva> getReservas();
    Optional findReservaById(int id);
    //update
    Reserva updateReserva(Reserva reserva);
    //delete
    void deleteReserva(int id);
    List<Reserva> findAll();
    Page<Reserva> findAll(Pageable pageable);
}
