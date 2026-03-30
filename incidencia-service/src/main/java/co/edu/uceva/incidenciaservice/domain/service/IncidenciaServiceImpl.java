package co.edu.uceva.incidenciaservice.domain.service;

import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.repository.IIncidenciaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IncidenciaServiceImpl implements IIncidenciaService {

    private final IIncidenciaRepository incidenciaRepository;

    public IncidenciaServiceImpl(IIncidenciaRepository incidenciaRepository) {
        this.incidenciaRepository = incidenciaRepository;
    }

    @Override
    public List<Incidencia> findAll() {
        return incidenciaRepository.findAll();
    }

    @Override
    public Optional<Incidencia> findById(Long id) {
        return incidenciaRepository.findById(id);
    }

    @Override
    public Incidencia save(Incidencia incidencia) {
        // TODO: MÁS ADELANTE AQUÍ INYECTAREMOS LA LÓGICA DE GEMINI (SPRING AI)
        // 1. Llamar al aula-service usando FeignClient para saber cómo se llama el aula.
        // 2. Comunicarnos con Gemini enviándole la descripción.
        // 3. Setear 'incidencia.setCartaFormalGenerada(respuestaGemini)'.
        
        return incidenciaRepository.save(incidencia);
    }

    @Override
    public Incidencia update(Incidencia incidencia) {
        return incidenciaRepository.save(incidencia);
    }

    @Override
    public void delete(Incidencia incidencia) {
        incidenciaRepository.delete(incidencia);
    }
}
