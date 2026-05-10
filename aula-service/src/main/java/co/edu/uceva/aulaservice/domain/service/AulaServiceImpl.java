package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.repository.IAulaRepository;
import co.edu.uceva.aulaservice.domain.model.Aula;
import co.edu.uceva.aulaservice.domain.repository.IBloqueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.util.List;
import java.util.Optional;

@Service
public class AulaServiceImpl  implements IAulaService {

    IAulaRepository repository;
    IBloqueRepository bloqueRepository;

    public AulaServiceImpl(IBloqueRepository bloqueRepository, IAulaRepository repository) {
        this.bloqueRepository = bloqueRepository;
        this.repository = repository;
    }

    @Override
    @Transactional
    public Aula save(Aula aula) {
        return repository.save(aula);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
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
        return repository.findById(codigoAula)
                .map(aula -> aula.getCodigoTipoAula().toString())
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Aula> obtenerAula(Long codigoAula) {
        return repository.findByCodigoAula(codigoAula);
    }

    @Override
    public List<Aula> filtrarPorBloque(Long bloqueId) {
        return repository.findByBloqueId(bloqueId);
    }

    @Override
    public List<Aula> filtrarPorFacultad(Long facultadId) {
        return repository.findByBloque_Facultad_Id(facultadId);
    }
}

