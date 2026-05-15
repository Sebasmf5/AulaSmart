package co.edu.uceva.aulaservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import co.edu.uceva.security.converter.EncryptDatabaseConverter;

@Entity
@Table(name = "aulas")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Aula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // dependo de stiven
    @Column(name = "codigoAula", nullable = false, unique = true)
    private Long codigoAula;

    @Column(name = "nombreAula", nullable = false)
    private String nombreAula;

    @Column(name = "codigoEdificio", nullable = false)
    private String codigoEdificio;

    @Column(name = "nombreEdificio", nullable = false, length = 512)
    @Convert(converter = EncryptDatabaseConverter.class)
    private String nombreEdificio;

    // cantidad de sillas del aula
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloque_id", nullable = false)
    private Bloque bloque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_aula_id", nullable = false)
    private TipoAula tipoAula;

    @Column(name = "sincronizada_con_siga")
    private Boolean sincronizadaConSiga = false;
}
