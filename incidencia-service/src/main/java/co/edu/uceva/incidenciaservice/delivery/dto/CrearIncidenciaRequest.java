package co.edu.uceva.incidenciaservice.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para la creación de incidencias.
 * El frontend solo debe enviar estos 3 campos.
 * El backend maneja automáticamente: codigoUsuario, estado, fechaReporte, cartaFormalGenerada.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearIncidenciaRequest {

    @NotNull(message = "El código del aula es obligatorio")
    private Long codigoAula;

    @NotBlank(message = "La descripción del daño es obligatoria para generar la carta")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
    private String descripcionBreve;

    @NotBlank(message = "El tipo de incidencia es obligatorio")
    private String tipoIncidencia; // HARDWARE, SOFTWARE, INFRAESTRUCTURA, OTRO
}
