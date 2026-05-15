package co.edu.uceva.aulaservice.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uceva.aulaservice.domain.model.TipoAula;

import java.util.Optional;

public interface ITipoAulaRepository extends JpaRepository<TipoAula, Long> {
    Optional<TipoAula> findByCodigoTipoAula(String codigoTipoAula);
    Optional<TipoAula> findByNombreContainingIgnoreCase(String nombre);
}
