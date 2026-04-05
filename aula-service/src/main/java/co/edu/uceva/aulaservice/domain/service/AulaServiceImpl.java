package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.repository.IAulaRepository;
import co.edu.uceva.aulaservice.domain.model.Aula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.util.List;
import java.util.Optional;

@Service
public class AulaServiceImpl  implements IAulaService {

    IAulaRepository repository;

    public AulaServiceImpl(IAulaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Aula save(Aula aula) {
        return repository.save(aula);
    }

    @Override
    @Transactional
    public void delete(Aula aula) {
        repository.delete(aula);
    }

    @Override
    @Transactional
    public Optional<Aula> findById(Long id) {
        return repository.findById(id);
    }


    @Override
    @Transactional
    public Aula update(Aula aula) {
        return repository.save(aula);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aula> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Aula> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerTipoAula(Long codigoAula) {
        return repository.findByCodigoAula(codigoAula)
                .map(aula -> aula.getCodigoTipoAula().toString())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Aula> obtenerAula(Long codigoAula) {
        return repository.findByCodigoAula(codigoAula);
    }
}

