package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.AutorDTO;
import com.biblioteca.backend.dto.AutorResponseDTO;
import com.biblioteca.backend.dto.PaisDTO;
import com.biblioteca.backend.exception.DatosInvalidosException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Autor;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.AutorRepository;
import com.biblioteca.backend.repository.PaisRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutorService {

    private final AutorRepository autorRepository;
    private final PaisRepository paisRepository;

    public AutorService(AutorRepository autorRepository, PaisRepository paisRepository) {
        this.autorRepository = autorRepository;
        this.paisRepository = paisRepository;
    }

    public List<AutorResponseDTO> listarTodos() {
        return autorRepository.findAll().stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    public AutorResponseDTO buscarPorId(Long id) {
        Autor autor = obtenerAutorOLanzarExcepcion(id);
        return convertirAResponseDTO(autor);
    }

    public AutorResponseDTO crear(AutorDTO dto) {
        Autor autor = new Autor();
        mapearDTOaEntidad(dto, autor);
        Autor guardado = autorRepository.save(autor);
        return convertirAResponseDTO(guardado);
    }

    public AutorResponseDTO actualizar(Long id, AutorDTO dto) {
        Autor autor = obtenerAutorOLanzarExcepcion(id);
        mapearDTOaEntidad(dto, autor);
        Autor actualizado = autorRepository.save(autor);
        return convertirAResponseDTO(actualizado);
    }

    public void eliminar(Long id) {
        Autor autor = obtenerAutorOLanzarExcepcion(id);
        autorRepository.delete(autor);
    }

    private Autor obtenerAutorOLanzarExcepcion(Long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Autor no encontrado con id: " + id));
    }


    // Lee un DTO
    private void mapearDTOaEntidad(AutorDTO dto, Autor autor) {
        Pais pais = paisRepository.findById(dto.getPaisId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "País no encontrado con id: " + dto.getPaisId()));
        
        validarFechasAutor(dto);

        autor.setNombre(dto.getNombre());
        autor.setIdioma(dto.getIdioma());
        autor.setPais(pais);
        autor.setRetratoUrl(dto.getRetratoUrl());
        autor.setFechaNacimiento(dto.getFechaNacimiento());
        autor.setAnioNacimientoAprox(dto.getAnioNacimientoAprox());
        autor.setFechaDefuncion(dto.getFechaDefuncion());
        autor.setAnioDefuncionAprox(dto.getAnioDefuncionAprox());
    }

    // Genera un DTO
    private AutorResponseDTO convertirAResponseDTO(Autor autor) {
        PaisDTO paisDTO = new PaisDTO(autor.getPais().getId(), autor.getPais().getNombre());
        return new AutorResponseDTO(
                autor.getId(),
                autor.getNombre(),
                autor.getIdioma(),
                paisDTO,
                autor.getRetratoUrl(),
                autor.getFechaNacimiento(),
                autor.getAnioNacimientoAprox(),
                autor.getFechaDefuncion(),
                autor.getAnioDefuncionAprox()
        );
    }

    // Valida las fechas ingresadas
    private void validarFechasAutor(AutorDTO dto) {
        if (dto.getFechaNacimiento() != null && dto.getAnioNacimientoAprox() != null) {
            throw new DatosInvalidosException(
                    "No se puede ingresar fecha de nacimiento exacta y año aproximado al mismo tiempo");
        }
        if (dto.getFechaDefuncion() != null && dto.getAnioDefuncionAprox() != null) {
            throw new DatosInvalidosException(
                    "No se puede ingresar fecha de defunción exacta y año aproximado al mismo tiempo");
        }
    }
}