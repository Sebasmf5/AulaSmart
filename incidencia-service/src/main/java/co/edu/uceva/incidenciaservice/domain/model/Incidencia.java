package co.edu.uceva.incidenciaservice.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación lógica con aula-service
    @Column(name = "codigo_aula", nullable = false)
    private Long codigoAula;

    // Relación lógica con usuarios-service (quién reportó)
    @Column(name = "codigo_usuario", nullable = false)
    private Long codigoUsuario;

    @NotBlank(message = "La descripción del daño es obligatoria para generar la carta")
    @Column(name = "descripcion_breve", nullable = false, length = 500)
    private String descripcionBreve;

    // Guardaremos la ruta de la imagen provista como evidencia (si aplica)
    @Column(name = "url_imagen")
    private String urlImagen;

    // Aquí guardaremos la carta formal devuelta mágicamente por Gemini
    @Column(name = "carta_formal_generada", columnDefinition = "TEXT")
    private String cartaFormalGenerada;

    @Column(name = "fecha_reporte", nullable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.fechaReporte = LocalDateTime.now();
    }
}
