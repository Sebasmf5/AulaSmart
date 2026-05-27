package co.edu.uceva.incidenciaservice.domain.service;

import co.edu.uceva.incidenciaservice.domain.model.EstadoIncidencia;
import co.edu.uceva.incidenciaservice.domain.model.Incidencia;
import co.edu.uceva.incidenciaservice.domain.repository.IIncidenciaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IncidenciaServiceImpl implements IIncidenciaService {

    private final IIncidenciaRepository incidenciaRepository;
    private final ChatGPTCartaService chatGPTCartaService;

    @Value("${app.uploads.incidencias:/app/uploads/incidencias}")
    private String uploadDir;

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
        // Generar carta formal con IA
        String cartaFormal = chatGPTCartaService.generarCartaFormal(
                incidencia.getDescripcionBreve(),
                incidencia.getTipoIncidencia().name(),
                incidencia.getCodigoAula()
        );
        incidencia.setCartaFormalGenerada(cartaFormal);

        // Estado por defecto
        if (incidencia.getEstado() == null) {
            incidencia.setEstado(EstadoIncidencia.PENDIENTE);
        }

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
        // Eliminar imagen asociada si existe
        if (incidencia.getUrlImagen() != null && !incidencia.getUrlImagen().isBlank()) {
            try {
                Path path = Paths.get(uploadDir).resolve(incidencia.getUrlImagen());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("[IncidenciaService] No se pudo eliminar imagen: " + e.getMessage());
            }
        }
        incidenciaRepository.delete(incidencia);
    }

    @Override
    public List<Incidencia> findByEstado(EstadoIncidencia estado) {
        return incidenciaRepository.findByEstado(estado);
    }

    @Override
    public long countByEstado(EstadoIncidencia estado) {
        return incidenciaRepository.countByEstado(estado);
    }

    @Override
    @Transactional
    public Incidencia guardarImagenEvidencia(Long incidenciaId, MultipartFile imagen) {
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada con id: " + incidenciaId));

        if (imagen == null || imagen.isEmpty()) {
            throw new IllegalArgumentException("La imagen no puede estar vacía");
        }

        try {
            // Crear directorio si no existe
            Path directorioUploads = Paths.get(uploadDir);
            if (!Files.exists(directorioUploads)) {
                Files.createDirectories(directorioUploads);
            }

            // Generar nombre único
            String extension = obtenerExtension(imagen.getOriginalFilename());
            String nombreArchivo = "incidencia_" + incidenciaId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Guardar archivo
            Path rutaArchivo = directorioUploads.resolve(nombreArchivo);
            Files.copy(imagen.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            // Guardar ruta relativa en BD
            incidencia.setUrlImagen(nombreArchivo);
            return incidenciaRepository.save(incidencia);

        } catch (IOException e) {
            System.err.println("[IncidenciaService] Error al guardar imagen: " + e.getMessage());
            System.err.println("[IncidenciaService] UploadDir: " + uploadDir);
            System.err.println("[IncidenciaService] Causa: " + e.getCause());
            e.printStackTrace();
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Incidencia responderIncidencia(Long incidenciaId, String respuesta, Long codigoAdministrador) {
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada con id: " + incidenciaId));

        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalArgumentException("La respuesta no puede estar vacía");
        }

        incidencia.setRespuestaAdministracion(respuesta);
        incidencia.setCodigoAdministrador(codigoAdministrador);
        incidencia.setFechaRespuesta(java.time.LocalDateTime.now());
        incidencia.setEstado(EstadoIncidencia.REVISADA);

        return incidenciaRepository.save(incidencia);
    }

    private String obtenerExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
