package co.edu.uceva.incidenciaservice.domain.repository;

import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IIncidenciaRepository extends JpaRepository<Incidencia, Long> {
}
