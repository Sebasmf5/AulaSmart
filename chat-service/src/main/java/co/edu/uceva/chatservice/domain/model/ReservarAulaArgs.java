package co.edu.uceva.chatservice.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReservarAulaArgs(
        @JsonPropertyDescription("Código del aula a reservar (opcional) Ej: 212")
        Long codigoAula,

        @JsonPropertyDescription("Nombre del Aula a reservar (obligatorio) Ej: B101- Digital o B101")
        String nombreAula,

        @JsonPropertyDescription("Fecha de la reserva en formato yyyy-MM-dd")
        String fecha,

        @JsonPropertyDescription("Hora de inicio en formato HH:mm")
        String horaInicio,

        @JsonPropertyDescription("Hora de fin en formato HH:mm")
        String horaFin
) {}
