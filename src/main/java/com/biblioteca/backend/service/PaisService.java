package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.PaisDTO;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.PaisRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaisService {

    private final PaisRepository paisRepository;

    public PaisService(PaisRepository paisRepository) {
        this.paisRepository = paisRepository;
    }

    public List<PaisDTO> listarTodos() {
        return paisRepository.findAll().stream()
                .map(pais -> new PaisDTO(pais.getId(), pais.getNombre()))
                .collect(Collectors.toList());
    }

    public PaisDTO crear(PaisDTO dto) {
        if (paisRepository.findByNombreIgnoreCase(dto.getNombre()).isPresent()) {
            throw new RecursoDuplicadoException("Ya existe un país con el nombre: " + dto.getNombre());
        }
        Pais pais = new Pais();
        pais.setNombre(dto.getNombre());
        Pais guardado = paisRepository.save(pais);
        return new PaisDTO(guardado.getId(), guardado.getNombre());
    }

    public void eliminar(Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("País no encontrado con id: " + id));
        paisRepository.delete(pais);
    }
}