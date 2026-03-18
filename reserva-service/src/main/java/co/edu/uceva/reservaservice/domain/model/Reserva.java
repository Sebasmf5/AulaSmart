package co.edu.uceva.reservaservice.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter

public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //dependo de stiven
    @Column(name = "codigo _aula", nullable = false)
    private Long codigoAula;

    @Column(name = "hora_inicio, nullable = false")
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin, nullable = false")
    private LocalDateTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadosReserva estado;

    @Column(name = "id_solicitante", nullable = false)
    private Long idSolicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_solicitante", nullable = false)
    private RolUsuario rolSolicitante;

    // Ej: "Ingeniería de Sistemas" o el ID del programa
    @Column(name = "codigo_programa")
    private String codigoPrograma;

    // Ej: "1" o "A"
    @Column(name = "grupo")
    private String grupo;

    @Version
    private Long version;
}
