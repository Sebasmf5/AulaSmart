package co.edu.uceva.aulaservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    // cantidad de sillas del aula
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloque_id", nullable = false)
    private Bloque bloque;

    @Column(name = "codigoTipoAula", nullable = false)
    private String codigoTipoAula;

    @Column(name = "nombreTipoAula", nullable = false)
    private String nombreTipoAula;

    @Column(name = "requiereAutorizacion", nullable = false)
    private Boolean requiereAutorizacion;
}
