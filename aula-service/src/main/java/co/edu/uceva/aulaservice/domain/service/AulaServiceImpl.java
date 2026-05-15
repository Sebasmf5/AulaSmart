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

    private static final long CODIGO_AULA_MANUAL_MIN = 900000L;

    @Override
    @Transactional
    public Aula save(Aula aula) {
        // Aulas manuales por defecto no están en SIGA
        if (aula.getSincronizadaConSiga() == null) {
            aula.setSincronizadaConSiga(false);
        }

        // Si es aula manual (no de SIGA) y no tiene codigoAula, generar uno automáticamente
        if (Boolean.FALSE.equals(aula.getSincronizadaConSiga()) && aula.getCodigoAula() == null) {
            Long maxCodigo = repository.findMaxCodigoAula();
            long nuevoCodigo = Math.max(CODIGO_AULA_MANUAL_MIN, (maxCodigo != null ? maxCodigo + 1 : CODIGO_AULA_MANUAL_MIN));
            aula.setCodigoAula(nuevoCodigo);
            System.out.println("[AulaServiceImpl] Aula manual generada con codigoAula automático: " + nuevoCodigo);
        }

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
        return repository.findByCodigoAula(codigoAula)
                .map(aula -> aula.getTipoAula().getCodigoTipoAula())
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
        return repository.findByBloque_Facultades_Id(facultadId);
    }

    @Override
    public List<Aula> filtrarPorNombre(String nombre) {
        return repository.findByNombreAulaContainingIgnoreCase(nombre);
    }

    @Override
    public List<Aula> filtrarPorTipoAula(String tipoAula) {
        return repository.findByTipoAula_NombreContainingIgnoreCase(tipoAula);
    }

    @Override
    public List<Aula> filtrarPorTipoAulaId(Long tipoAulaId) {
        return repository.findByTipoAulaId(tipoAulaId);
    }
}

