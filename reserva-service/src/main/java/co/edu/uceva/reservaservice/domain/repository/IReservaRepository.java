package co.edu.uceva.reservaservice.domain.repository;

import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReservaRepository extends JpaRepository<Reserva, Long> {
}
