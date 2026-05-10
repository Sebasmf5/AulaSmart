package co.edu.uceva.aulaservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uceva.aulaservice.domain.model.Facultad;

import java.util.Optional;

public interface IFacultadRepository extends JpaRepository<Facultad, Long> {
    Optional<Facultad> findByCodigoDependencia(String codigoDependencia);
}
