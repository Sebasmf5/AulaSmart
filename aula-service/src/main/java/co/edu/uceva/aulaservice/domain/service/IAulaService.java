package co.edu.uceva.aulaservice.domain.service;

import co.edu.uceva.aulaservice.domain.model.Aula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IAulaService {

        List<Aula> findAll();
        Page<Aula> findAll(Pageable pageable);
}
