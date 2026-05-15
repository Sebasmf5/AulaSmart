package co.edu.uceva.aulaservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uceva.aulaservice.domain.model.Bloque;

import java.util.List;
import java.util.Optional;

public interface IBloqueRepository extends JpaRepository<Bloque, Long> {
    Optional<Bloque> findByCodigoEdificio(String codigoEdificio);
    List<Bloque> findByFacultades_Id(Long facultadId);
    Bloque findFirstByNombreContainingIgnoreCase(String nombre);
}
