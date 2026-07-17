package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.*;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.*;
import com.biblioteca.backend.repository.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

@Service
public class LibroService {

    // Importación de repositorios.
    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final GeneroRepository generoRepository;

    // Constructor del servicio.
    public LibroService(LibroRepository libroRepository,
                         AutorRepository autorRepository,
                         GeneroRepository generoRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.generoRepository = generoRepository;
    }

    // Métodos.
    // Listar todos.
    public List<LibroResponseDTO> listarTodos() {
        return libroRepository.findAll().stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    // Lista por filtros ingresados.
    public List<LibroResponseDTO> buscarConFiltros(LibroFiltroDTO filtro) {
        Specification<Libro> spec = Specification  // Revisa que filtros tienen datos.
                .where(LibroSpecification.tieneEstado(filtro.getEstado()))
                .and(LibroSpecification.tieneAlgunGenero(filtro.getGeneroIds()))
                .and(LibroSpecification.tienePaisAutor(filtro.getPaisAutorId()))
                .and(LibroSpecification.tieneIdiomaAutor(filtro.getIdiomaAutor()))
                .and(LibroSpecification.textoLibre(filtro.getTexto()));

        return libroRepository.findAll(spec).stream()  // Retorna una lista de los objetos encontrados.
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    // Busca por id.
    public LibroResponseDTO buscarPorId(Long id) {
        Libro libro = obtenerLibroOLanzarExcepcion(id);
        return convertirAResponseDTO(libro);
    }

    // Crear nuevo.
    public LibroResponseDTO crear(LibroDTO dto) {
        // Revisamos que el isbn fue ingresado, y si fue ingresado, que no esté repetido.
        if (dto.getIsbn() != null && !dto.getIsbn().isBlank()
                && libroRepository.findByIsbn(dto.getIsbn()).isPresent()) {  // Si hay problemas lanzamos la excepción.
            throw new RecursoDuplicadoException("Ya existe un libro con el ISBN: " + dto.getIsbn());
        }
        // Creamos.
        Libro libro = new Libro();
        mapearDTOaEntidad(dto, libro);
        Libro guardado = libroRepository.save(libro);
        return convertirAResponseDTO(guardado);
    }

    // Actualiza.
    public LibroResponseDTO actualizar(Long id, LibroDTO dto) {
    Libro libro = obtenerLibroOLanzarExcepcion(id);  // Buscamos el libro que vamos a editar.
    // Revisamos que el isbn fue ingresado, y si fue ingresado, que no esté repetido.
    if (dto.getIsbn() != null && !dto.getIsbn().isBlank()) {  // Si se ingresó un isbn nuevo.
        libroRepository.findByIsbn(dto.getIsbn())  // Buscamos en la base de datos por el isbn nuevo (para ver si ya existe).
                .filter(libroExistente -> !libroExistente.getId().equals(id))  // si se encuentra el libro por el isbn nuevo y no es el que estamos editando.
                .ifPresent(libroExistente -> {  // Lanzamos una excepción.
                    throw new RecursoDuplicadoException("Ya existe un libro con el ISBN: " + dto.getIsbn());
                });
    }
    // Actualizamos.
    mapearDTOaEntidad(dto, libro);
    Libro actualizado = libroRepository.save(libro);
    return convertirAResponseDTO(actualizado);
}

    // Elimina.
    public void eliminar(Long id) {
        Libro libro = obtenerLibroOLanzarExcepcion(id);
        libroRepository.delete(libro);
    }


    // --- Métodos privados de apoyo ---
    // Resuelve excepciones.
    private Libro obtenerLibroOLanzarExcepcion(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado con id: " + id));
    }

    // Método que revisa si el estado ingresado es válido. ((( Requiere edición si se agregan nuevos estados )))
    private EstadoLibro parsearEstado(String estado) {
    try {
        return EstadoLibro.valueOf(estado.toUpperCase());
    } catch (IllegalArgumentException e) {
        throw new RecursoNoEncontradoException(
            "Estado inválido: " + estado + ". Valores permitidos: POR_LEER, LEYENDO, LEIDO");
    }
}

    // Escritura y lectura de DTOS.
    // Mapea los datos ingresados al DTO de libro.
    private void mapearDTOaEntidad(LibroDTO dto, Libro libro) {
        Autor autor = autorRepository.findById(dto.getAutorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Autor no encontrado con id: " + dto.getAutorId()));

        libro.setTitulo(dto.getTitulo());
        libro.setIsbn(dto.getIsbn());
        libro.setPortadaUrl(dto.getPortadaUrl());
        libro.setEstado(parsearEstado(dto.getEstado()));
        libro.setAutor(autor);

        if (dto.getGeneroIds() != null && !dto.getGeneroIds().isEmpty()) {
            Set<Genero> generos = new HashSet<>(generoRepository.findAllById(dto.getGeneroIds()));
            libro.setGeneros(generos);
        } else {
            libro.setGeneros(new HashSet<>());
        }
    }

    // Genera un DTO de respuesta con uno o muchos libros.
    private LibroResponseDTO convertirAResponseDTO(Libro libro) {
        AutorResumenDTO autorDTO = new AutorResumenDTO(
                libro.getAutor().getId(),
                libro.getAutor().getNombre(),
                libro.getAutor().getIdioma(),
                libro.getAutor().getPais() != null ? libro.getAutor().getPais().getNombre() : null
        );

        List<String> nombresGeneros = libro.getGeneros().stream()
                .map(Genero::getNombre)
                .collect(Collectors.toList());

        return new LibroResponseDTO(
                libro.getId(),
                libro.getTitulo(),
                libro.getIsbn(),
                libro.getPortadaUrl(),
                libro.getEstado().name(),
                autorDTO,
                nombresGeneros
        );
    }
}