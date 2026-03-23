package co.edu.uceva.aulaservice.domain.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.uceva.aulaservice.domain.model.Aula;


public interface IAulaRepository extends JpaRepository<Aula, Long> {
}
