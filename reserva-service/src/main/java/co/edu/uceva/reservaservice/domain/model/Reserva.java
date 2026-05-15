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

    @Column(name = "aula_id", nullable = false)
    @NotNull(message = "El ID del aula es obligatorio")
    private Long aulaId;

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
    private EstadosReserva estado;

    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_solicitante", nullable = false)
    private RolUsuario rolSolicitante;

    @Column(name = "codigo_programa")
    private String codigoPrograma;

    @Column(name = "grupo", length = 512)
    @NotBlank(message = "El grupo no puede estar vacío")
    private String grupo;

    @Column(name = "nombre_usuario_responsable")
    private String nombreUsuarioResponsable;

    @Column(name = "titulo")
    @NotBlank(message = "El motivo o título de la reserva es obligatorio")
    private String titulo;

    @Version
    private Long version;
}
