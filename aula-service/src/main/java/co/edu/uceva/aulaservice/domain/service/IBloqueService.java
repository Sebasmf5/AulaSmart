package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.Bloque;
import java.util.List;

public interface IBloqueService {
    List<Bloque> listarTodos();
    List<Bloque> listarPorFacultad(Long facultadId);
}
