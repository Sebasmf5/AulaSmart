package co.edu.uceva.usuariosservice.domain.service;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService{

    IUsuarioRepository repository;

    public UsuarioServiceImpl(IUsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Usuario> findAll() {
        return repository.findAll();
    }

    @Override
    public Usuario findById(long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public Usuario update(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return repository.save(usuario);
    }

    @Override
    public void delete(Usuario usuario) {
        repository.delete(usuario);
    }

    @Override
    public Page<Usuario> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
