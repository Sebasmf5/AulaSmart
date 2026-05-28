package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.TipoAula;
import co.edu.uceva.aulaservice.domain.repository.ITipoAulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoAulaServiceImpl implements ITipoAulaService {

    private final ITipoAulaRepository tipoAulaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoAula> listarTodos() {
        return tipoAulaRepository.findAll();
    }
}
