package co.edu.uceva.incidenciaservice.domain.service;

import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.repository.IIncidenciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class IncidenciaServiceImpl implements IIncidenciaService {

    private final IIncidenciaRepository incidenciaRepository;
    private final ChatGPTCartaService chatGPTCartaService;

    public IncidenciaServiceImpl(IIncidenciaRepository incidenciaRepository,
                                 ChatGPTCartaService chatGPTCartaService) {
        this.incidenciaRepository = incidenciaRepository;
        this.chatGPTCartaService = chatGPTCartaService;
    }

    @Override
    public List<Incidencia> findAll() {
        return incidenciaRepository.findAll();
    }

    @Override
    public Page<Incidencia> findAll(Pageable pageable) {
        return incidenciaRepository.findAll(pageable);
    }

    @Override
    public Optional<Incidencia> findById(Long id) {
        return incidenciaRepository.findById(id);
    }

    @Override
    @Transactional
    public Incidencia save(Incidencia incidencia) {
        // Llamamos a la IA (Groq/OpenAI) para generar la carta formal a partir de los puntos clave.
        // Si la IA falla (rate-limit, error de red, etc.) el método devuelve null
        // y la incidencia se guarda igualmente sin carta (fallback graceful).
        String cartaFormal = chatGPTCartaService.generarCartaFormal(
                incidencia.getDescripcionBreve(),
                incidencia.getTipoIncidencia().name(),
                incidencia.getCodigoAula()
        );
        incidencia.setCartaFormalGenerada(cartaFormal);

        return incidenciaRepository.save(incidencia);
    }

    @Override
    @Transactional
    public Incidencia update(Incidencia incidencia) {
        return incidenciaRepository.save(incidencia);
    }

    @Override
    @Transactional
    public void delete(Incidencia incidencia) {
        incidenciaRepository.delete(incidencia);
    }
}
