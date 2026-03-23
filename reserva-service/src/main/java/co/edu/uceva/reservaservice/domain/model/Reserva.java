package co.edu.uceva.reservaservice.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter

public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @Column(name = "codigo_aula", nullable = false)
    @NotNull(message = "El código del aula es obligatorio")
    private Long codigoAula;

    @Column(name = "hora_inicio", nullable = false)
    @NotNull(message = "La hora de inicio es obligatoria")
    @FutureOrPresent(message = "La reserva no puede ser en el pasado")
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    @NotNull(message = "La hora de fin es obligatoria")
    @FutureOrPresent(message = "La hora de fin debe ser una fecha presente o futura")
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "El estado de la reserva es obligatorio")
    private EstadosReserva estado;

    @Column(name = "id_solicitante", nullable = false)
    @NotNull(message = "El ID del solicitante es obligatorio")
    private Long idSolicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_solicitante", nullable = false)
    @NotNull(message = "El rol del solicitante es obligatorio")
    private RolUsuario rolSolicitante;

    @Column(name = "codigo_programa")
    private String codigoPrograma;

    @Column(name = "grupo")
    @NotBlank(message = "El grupo no puede estar vacío")
    private String grupo;

    @Version
    private Long version;
}
