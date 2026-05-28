package co.edu.uceva.incidenciaservice.domain.service;

import co.edu.uceva.incidenciaservice.domain.model.EstadoIncidencia;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface IIncidenciaService {
    List<Incidencia> findAll();
    Page<Incidencia> findAll(Pageable pageable);
    Optional<Incidencia> findById(Long id);
    Incidencia save(Incidencia incidencia);
    Incidencia update(Incidencia incidencia);
    void delete(Incidencia incidencia);

    // Nuevos métodos
    List<Incidencia> findByEstado(EstadoIncidencia estado);
    long countByEstado(EstadoIncidencia estado);
    Incidencia guardarImagenEvidencia(Long incidenciaId, MultipartFile imagen);
    Incidencia responderIncidencia(Long incidenciaId, String respuesta, Long codigoAdministrador);
}
