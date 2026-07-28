package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.ConteoDobleDTO;
import com.biblioteca.backend.dto.SugerenciaLibroDTO;
import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.model.Libro;
import com.biblioteca.backend.repository.LibroRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class RecomendacionesService {

    private static final int CANTIDAD_SUGERENCIAS = 5;

    private final LibroRepository libroRepository;
    private final EstadisticaService estadisticaService;
    private final Random random = new Random();

    public RecomendacionesService(LibroRepository libroRepository, EstadisticaService estadisticaService) {
        this.libroRepository = libroRepository;
        this.estadisticaService = estadisticaService;
    }

    public List<SugerenciaLibroDTO> obtenerPorAutorPendiente() {
        // Ranking de autores por cantidadLeidos descendente (regla de negocio ya definida en EstadisticaService).
        List<ConteoDobleDTO> rankingAutores = estadisticaService.obtenerConteoPorAutor(null);

        // Una sola query: todos los POR_LEER con su autor, agrupados en memoria por autorId.
        Map<Long, List<Libro>> pendientesPorAutor = libroRepository
                .findByEstadoConAutor(EstadoLibro.POR_LEER).stream()
                .collect(Collectors.groupingBy(libro -> libro.getAutor().getId()));

        List<SugerenciaLibroDTO> sugerencias = new ArrayList<>();

        for (ConteoDobleDTO autor : rankingAutores) {
            if (sugerencias.size() >= CANTIDAD_SUGERENCIAS) {
                break;
            }
            List<Libro> pendientes = pendientesPorAutor.get(autor.getAutorId());
            if (pendientes == null || pendientes.isEmpty()) {
                continue; // autor sin pendientes: se salta, no gasta cupo
            }
            Libro elegido = pendientes.get(random.nextInt(pendientes.size()));
            sugerencias.add(new SugerenciaLibroDTO(
                    elegido.getId(),
                    elegido.getTitulo(),
                    elegido.getAutor().getNombre(),
                    elegido.getPortadaUrl()
            ));
        }

        return sugerencias;
    }
}