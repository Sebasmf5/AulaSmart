package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.Bloque;
import co.edu.uceva.aulaservice.domain.repository.IBloqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueServiceImpl implements IBloqueService {

    private final IBloqueRepository bloqueRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Bloque> listarTodos() {
        return bloqueRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bloque> listarPorFacultad(Long facultadId) {
        return bloqueRepository.findByFacultades_Id(facultadId);
    }

    @Override
    public Bloque filtrarPorNombre(String nombre) {
        return bloqueRepository.findFirstByNombreContainingIgnoreCase(nombre);
    }
}
