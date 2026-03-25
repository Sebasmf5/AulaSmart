package co.edu.uceva.usuariosservice.domain.repository;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUsuarioRepository extends JpaRepository<Usuario, Long> {
}
