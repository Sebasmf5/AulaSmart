package co.edu.uceva.aulaservice.domain.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uceva.aulaservice.domain.model.Aula;

import java.util.List;
import java.util.Optional;


public interface IAulaRepository extends JpaRepository<Aula, Long> {
    Optional<Aula> findByCodigoAula(Long codigoAula);
    List<Aula> findByBloqueId(Long bloqueId);
    List<Aula> findByBloque_Facultades_Id(Long facultadId);
    List<Aula> findByNombreAulaContainingIgnoreCase(String texto);
    List<Aula> findByTipoAula_NombreContainingIgnoreCase(String nombreTipoAula);
    List<Aula> findByTipoAulaId(Long tipoAulaId);
    List<Aula> findBySincronizadaConSigaTrue();
}
