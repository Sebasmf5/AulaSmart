package co.edu.uceva.aulaservice.domain.integration.SigaDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Este DTO es para mapear la respuesta exacta en JSON
 * que devuelve el endpoint de la Universidad.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AulaExternaDto {

    @JsonProperty("codigo_aula")
    private Long codigoAula;

    @JsonProperty("nombre_aula")
    private String nombreAula;

    @JsonProperty("sillas")
    private Integer capacidad;

    @JsonProperty("codigo_edificio")
    private String codigoEdificio;

    @JsonProperty("nombre_edificio")
    private String nombreEdificio;

    @JsonProperty("codigo_dependencia")
    private String codigoDependencia;

    @JsonProperty("nombre_dependencia")
    private String nombreDependencia;

    @JsonProperty("codigo_tipo_aula")
    private String codigoTipoAula;

    @JsonProperty("nombre_tipo_aula")
    private String nombreTipoAula;

    // Campos extra que envía la API (opcionales, pero es buena práctica tenerlos)
    @JsonProperty("view")
    private String view;

    @JsonProperty("iconcls")
    private String iconcls;

    // Campo propio de nuestra lógica de negocio (no viene en el JSON)
    // Se inicializa en null o false en tu capa de servicio al guardar en BD.
    private Boolean requiereAutorizacion;
}