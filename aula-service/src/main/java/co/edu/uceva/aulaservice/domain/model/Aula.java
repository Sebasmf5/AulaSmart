package co.edu.uceva.aulaservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "aulas")
@Getter
@Setter

public class Aula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //dependo de stiven
    @Column(name = "codigoAula", nullable = false)
    private Long codigoAula;

    @Column(name = "nombreAula", nullable = false)
    private String nombreAula;

    @Column(name = "codigoEdificio", nullable = false)
    private String codigoEdificio;

    @Column(name = "nombreEdificio", nullable = false)
    private String nombreEdificio;

    //cantidad de sillas del aula
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "codigoDependencia", nullable = false)
    private String codigoDependencia;

    @Column(name = "nombreDependencia", nullable = false)
    private String nombreDependencia;

    @Column(name= "codigoTipoAula", nullable = false)
    private String codigoTipoAula;

    @Column(name = "nombreTipoAula", nullable = false)
    private String nombreTipoAula;

    @Version
    private Integer version;

}
