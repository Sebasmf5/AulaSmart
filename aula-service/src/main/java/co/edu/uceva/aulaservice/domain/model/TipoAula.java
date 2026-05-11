package co.edu.uceva.aulaservice.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "tipos_aula")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TipoAula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Correspondiente al codigo_tipo_aula del SIGA
    @Column(name = "codigo_tipo_aula", nullable = false, unique = true)
    private String codigoTipoAula;

    // Correspondiente al nombre_tipo_aula del SIGA
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "requiere_autorizacion", nullable = false)
    private Boolean requiereAutorizacion = false;

    @OneToMany(mappedBy = "tipoAula", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Aula> aulas;
}
