package co.edu.uceva.usuariosservice.domain.service;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUsuarioService {
     List<Usuario> findAll();
     Usuario findById(long id);
     Usuario update(Usuario usuario);
     Usuario save(Usuario usuario);
     void delete(Usuario usuario);
     Page<Usuario> findAll(Pageable pageable);
}
