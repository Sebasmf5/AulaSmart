package co.edu.uceva.chatservice.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TipoAulaDTO(
    Long id,
    String codigoTipoAula,
    String nombre,
    Boolean requiereAutorizacion
) {}
