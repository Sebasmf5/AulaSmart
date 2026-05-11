package co.edu.uceva.chatservice.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BloqueDTO(
    Long id,
    String codigoEdificio,
    String nombre
) {}
