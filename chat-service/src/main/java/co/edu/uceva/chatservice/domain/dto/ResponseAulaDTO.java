package co.edu.uceva.chatservice.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResponseAulaDTO (
        List<AulaDTO> aulas,
        String mensaje
)
{}
