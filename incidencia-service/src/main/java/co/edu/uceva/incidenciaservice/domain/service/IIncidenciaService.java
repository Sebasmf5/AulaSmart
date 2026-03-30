package co.edu.uceva.incidenciaservice.domain.service;

import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import java.util.List;
import java.util.Optional;

public interface IIncidenciaService {
    List<Incidencia> findAll();
    Optional<Incidencia> findById(Long id);
    Incidencia save(Incidencia incidencia);
    Incidencia update(Incidencia incidencia);
    void delete(Incidencia incidencia);
}
