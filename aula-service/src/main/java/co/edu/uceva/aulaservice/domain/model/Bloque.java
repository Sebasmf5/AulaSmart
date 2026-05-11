package co.edu.uceva.aulaservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "bloques")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bloque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Correspondiente al codigoEdificio del SIGA
    @Column(name = "codigo_edificio", nullable = false, unique = true)
    private String codigoEdificio;

    // Correspondiente al nombreEdificio del SIGA
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "bloque_facultad",
        joinColumns = @JoinColumn(name = "bloque_id"),
        inverseJoinColumns = @JoinColumn(name = "facultad_id")
    )
    private List<Facultad> facultades = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "bloque", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Aula> aulas;
}
