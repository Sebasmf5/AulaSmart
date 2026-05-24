package co.edu.uceva.incidenciaservice.domain.repository;

import co.edu.uceva.incidenciaservice.domain.model.EstadoIncidencia;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IIncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findByEstado(EstadoIncidencia estado);
    long countByEstado(EstadoIncidencia estado);
}
