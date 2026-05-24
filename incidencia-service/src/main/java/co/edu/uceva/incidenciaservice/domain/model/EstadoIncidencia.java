package co.edu.uceva.incidenciaservice.domain.model;

public enum EstadoIncidencia {
    PENDIENTE,      // Carta creada, esperando respuesta administrativa
    REVISADA,       // El administrador respondió
    CERRADA         // El usuario marcó como resuelta o se cerró el caso
}
