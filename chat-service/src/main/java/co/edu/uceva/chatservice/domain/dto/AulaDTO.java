package co.edu.uceva.chatservice.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AulaDTO(
    Long id,
    Long codigoAula,
    String nombreAula,
    Integer capacidad,
    BloqueDTO bloque,
    TipoAulaDTO tipoAula
) {}
