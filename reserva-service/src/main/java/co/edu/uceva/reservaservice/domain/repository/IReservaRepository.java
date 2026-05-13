package co.edu.uceva.reservaservice.domain.repository;

import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByAulaId(Long aulaId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r " +
            "WHERE r.aulaId = :aulaId " +
            "AND r.estado != 'CANCELADA' " +
            "AND r.horaInicio < :horaFin " +
            "AND r.horaFin > :horaInicio")
    boolean existeCruceDeHorarios(
            @Param("aulaId") Long aulaId,
            @Param("horaInicio") LocalDateTime horaInicio,
            @Param("horaFin") LocalDateTime horaFin
    );

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r " +
            "WHERE r.aulaId = :aulaId " +
            "AND r.idReserva != :idReserva " +
            "AND r.estado != 'CANCELADA' " +
            "AND r.horaInicio < :horaFin " +
            "AND r.horaFin > :horaInicio")
    boolean existeCruceDeHorariosUpdate(
            @Param("aulaId") Long aulaId,
            @Param("horaInicio") LocalDateTime horaInicio,
            @Param("horaFin") LocalDateTime horaFin,
            @Param("idReserva") Long idReserva
    );

    @Query("SELECT r.aulaId FROM Reserva r " +
            "WHERE r.estado != 'CANCELADA' " +
            "AND r.horaInicio < :horaFin " +
            "AND r.horaFin > :horaInicio")
    List<Long> findAulasOcupadasEnRango(
            @Param("horaInicio") LocalDateTime horaInicio,
            @Param("horaFin") LocalDateTime horaFin
    );
}
