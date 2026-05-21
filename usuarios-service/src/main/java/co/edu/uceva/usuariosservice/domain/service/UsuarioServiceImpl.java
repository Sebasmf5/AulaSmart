package co.edu.uceva.usuariosservice.domain.service;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements IUsuarioService{

    IUsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(IUsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public List<Usuario> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Usuario findById(long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Usuario update(Usuario usuario) {
        encodePasswordIfNeeded(usuario);
        return repository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario save(Usuario usuario) {
        encodePasswordIfNeeded(usuario);
        return repository.save(usuario);
    }

    private void encodePasswordIfNeeded(Usuario usuario) {
        String password = usuario.getPassword();
        if (password != null && !password.isBlank() && !password.startsWith("$2a$")) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
    }

    @Override
    @Transactional
    public void delete(Usuario usuario) {
        repository.delete(usuario);
    }

    @Override
    @Transactional
    public Page<Usuario> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
