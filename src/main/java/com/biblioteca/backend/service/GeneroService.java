package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.GeneroDTO;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.repository.GeneroRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeneroService {

    private final GeneroRepository generoRepository;

    public GeneroService(GeneroRepository generoRepository) {
        this.generoRepository = generoRepository;
    }

    public List<GeneroDTO> listarTodos() {
        return generoRepository.findAll().stream()
                .map(genero -> new GeneroDTO(genero.getId(), genero.getNombre()))
                .collect(Collectors.toList());
    }

    public GeneroDTO crear(GeneroDTO dto) {
        if (generoRepository.findByNombreIgnoreCase(dto.getNombre()).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un género con el nombre: " + dto.getNombre());
        }
        Genero genero = new Genero();
        genero.setNombre(dto.getNombre());
        Genero guardado = generoRepository.save(genero);
        return new GeneroDTO(guardado.getId(), guardado.getNombre());
    }

    public void eliminar(Long id) {
        Genero genero = generoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Género no encontrado con id: " + id));
        generoRepository.delete(genero);
    }
}
