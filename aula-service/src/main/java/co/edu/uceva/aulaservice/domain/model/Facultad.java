package co.edu.uceva.aulaservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "facultades")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Correspondiente al codigoDependencia del SIGA
    @Column(name = "codigo_dependencia", nullable = false, unique = true)
    private String codigoDependencia;

    // Correspondiente al nombreDependencia del SIGA
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToMany(mappedBy = "facultades")
    @JsonIgnore
    private List<Bloque> bloques;
}
