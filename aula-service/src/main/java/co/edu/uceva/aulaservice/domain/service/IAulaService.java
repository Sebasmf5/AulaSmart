package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.Aula;
import lombok.extern.java.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IAulaService {
        Aula save(Aula aula);
        void deleteById(Long id);
        Optional<Aula> findById(Long id);
        Aula update(Aula aula);
        List<Aula> findAll();
        Page<Aula> findAll(Pageable pageable);
        String obtenerTipoAula(Long codigo);
        Optional<Aula> obtenerAula(Long codigo);
}
