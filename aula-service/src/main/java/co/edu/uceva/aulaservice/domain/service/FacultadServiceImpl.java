package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.Facultad;
import co.edu.uceva.aulaservice.domain.repository.IFacultadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultadServiceImpl implements IFacultadService {

    private final IFacultadRepository facultadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Facultad> listarTodas() {
        return facultadRepository.findAll();
    }
}
