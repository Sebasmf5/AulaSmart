package co.edu.uceva.reservaservice.domain.repository;

import co.edu.uceva.reservaservice.domain.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IReservaRepository extends JpaRepository<Reserva, Long> {
// Consulta que devuelve 'true' si encuentra un choque de horarios
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r " +
            "WHERE r.codigoAula = :codigoAula " +
            "AND r.estado != 'CANCELADA' " +
            "AND r.horaInicio < :horaFin " +
            "AND r.horaFin > :horaInicio")
    boolean existeCruceDeHorarios(
            @Param("codigoAula") Long codigoAula,
            @Param("horaInicio") LocalDateTime horaInicio,
            @Param("horaFin") LocalDateTime horaFin
    );
}
