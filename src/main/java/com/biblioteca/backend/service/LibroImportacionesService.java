package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.*;
import com.biblioteca.backend.dto.importacion.*;
import com.biblioteca.backend.exception.DatosInvalidosException;
import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.GeneroRepository;
import com.biblioteca.backend.repository.PaisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la importación de un libro externo (vía Google Books, resuelto
 * en agentes-ia) al modelo interno: país -> autor -> géneros -> libro.
 *
 * No reusa AutorService.crear()/PaisService.crear()/GeneroService.crear()
 * para país y género porque esos métodos lanzan RecursoDuplicadoException
 * si el nombre ya existe (comportamiento correcto para el CRUD manual,
 * pero este flujo necesita semántica "buscar o crear" idempotente —
 * especialmente para PAIS_PENDIENTE, que se reusa en cada importación
 * donde el autor no trae país). Para autor sí se reusa AutorService.crear(),
 * porque autor nuevo nunca tuvo chequeo de duplicado. Para el libro final
 * se reusa LibroService.crear() completo (ya valida ISBN duplicado y fechas).
 */
@Service
public class LibroImportacionesService {

    private static final String PAIS_PLACEHOLDER = "PAIS_PENDIENTE";

    private final PaisRepository paisRepository;
    private final GeneroRepository generoRepository;
    private final AutorService autorService;
    private final LibroService libroService;

    public LibroImportacionesService(PaisRepository paisRepository,
                                      GeneroRepository generoRepository,
                                      AutorService autorService,
                                      LibroService libroService) {
        this.paisRepository = paisRepository;
        this.generoRepository = generoRepository;
        this.autorService = autorService;
        this.libroService = libroService;
    }

    @Transactional
    public LibroResponseDTO importarLibroExterno(ImportarLibroExternoDTO dto) {
        Long autorId = resolverAutor(dto.getAutor());
        List<Long> generoIds = resolverGeneros(dto.getGeneros());

        LibroDTO libroDTO = new LibroDTO();
        libroDTO.setTitulo(dto.getTitulo());
        libroDTO.setIsbn(dto.getIsbn());
        libroDTO.setPortadaUrl(dto.getPortadaUrl());
        libroDTO.setEstado(dto.getEstado() != null ? dto.getEstado() : "POR_LEER");
        libroDTO.setAutorId(autorId);
        libroDTO.setGeneroIds(generoIds);
        libroDTO.setAnioPublicacion(dto.getAnioPublicacion());
        // Lectura personal — opcionales, null si el usuario no los llenó
        // (libro por leer o leyendo, no aplica todavía).
        libroDTO.setAnioLectura(dto.getAnioLectura());
        libroDTO.setFechaInicio(dto.getFechaInicio());
        libroDTO.setFechaTermino(dto.getFechaTermino());

        return libroService.crear(libroDTO);
    }

    // --- Autor ---

    private Long resolverAutor(AutorImportDTO autorImport) {
        if (autorImport == null) {
            throw new DatosInvalidosException("El autor es obligatorio");
        }
        boolean tieneId = autorImport.getAutorId() != null;
        boolean tieneDatos = autorImport.getDatos() != null;
        if (tieneId == tieneDatos) {
            throw new DatosInvalidosException(
                    "El autor debe traer autorId (existente) o datos (nuevo), no ambos ni ninguno");
        }

        if (tieneId) {
            return autorImport.getAutorId();
        }

        AutorNuevoDTO datos = autorImport.getDatos();
        Long paisId = resolverPaisAutor(datos.getPais());

        AutorDTO autorDTO = new AutorDTO();
        autorDTO.setNombre(datos.getNombre());
        autorDTO.setIdioma(datos.getIdioma());
        autorDTO.setPaisId(paisId);
        autorDTO.setRetratoUrl(datos.getRetratoUrl());
        autorDTO.setFechaNacimiento(datos.getFechaNacimiento());
        autorDTO.setAnioNacimientoAprox(datos.getAnioNacimientoAprox());
        autorDTO.setFechaDefuncion(datos.getFechaDefuncion());
        autorDTO.setAnioDefuncionAprox(datos.getAnioDefuncionAprox());

        AutorResponseDTO creado = autorService.crear(autorDTO);
        return creado.getId();
    }

    // --- País (buscar o crear, con placeholder cuando no hay dato) ---

    private Long resolverPaisAutor(PaisImportDTO paisImport) {
        if (paisImport != null && paisImport.getPaisId() != null) {
            return paisImport.getPaisId();
        }

        String nombreDesdeDatos = (paisImport != null && paisImport.getDatos() != null)
                ? paisImport.getDatos().getNombre()
                : null;

        String nombre = (nombreDesdeDatos != null && !nombreDesdeDatos.isBlank())
                ? nombreDesdeDatos
                : PAIS_PLACEHOLDER;

        return buscarOCrearPais(nombre);
    }

    private Long buscarOCrearPais(String nombre) {
        return paisRepository.findByNombreIgnoreCase(nombre)
                .map(Pais::getId)
                .orElseGet(() -> {
                    Pais nuevo = new Pais();
                    nuevo.setNombre(nombre);
                    return paisRepository.save(nuevo).getId();
                });
    }

    // --- Géneros (buscar o crear cada uno) ---

    private List<Long> resolverGeneros(List<GeneroImportDTO> generosImport) {
        List<Long> ids = new ArrayList<>();
        if (generosImport == null) {
            return ids;
        }

        for (GeneroImportDTO generoImport : generosImport) {
            boolean tieneId = generoImport.getGeneroId() != null;
            boolean tieneDatos = generoImport.getDatos() != null;
            if (tieneId == tieneDatos) {
                throw new DatosInvalidosException(
                        "Cada género debe traer generoId (existente) o datos (nuevo), no ambos ni ninguno");
            }

            if (tieneId) {
                ids.add(generoImport.getGeneroId());
            } else {
                ids.add(buscarOCrearGenero(generoImport.getDatos()));
            }
        }

        return ids;
    }

    private Long buscarOCrearGenero(GeneroNuevoDTO datos) {
        return generoRepository.findByNombreIgnoreCase(datos.getNombre())
                .map(Genero::getId)
                .orElseGet(() -> {
                    Genero nuevo = new Genero();
                    nuevo.setNombre(datos.getNombre());
                    nuevo.setIconoSlug(datos.getIconoSlug());
                    return generoRepository.save(nuevo).getId();
                });
    }
}