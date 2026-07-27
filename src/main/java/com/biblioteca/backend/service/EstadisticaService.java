package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.ConteoDTO;
import com.biblioteca.backend.dto.RitmoLecturaDTO;
import com.biblioteca.backend.repository.LibroRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstadisticaService {

    // Importación de repositorios.
    private final LibroRepository libroRepository;

    // Constructor del servicio.
    public EstadisticaService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    // Métodos.
    // Conteo por estado (POR_LEER, LEYENDO, LEIDO).
    public List<ConteoDTO> obtenerConteoPorEstado() {
        return libroRepository.contarPorEstado().stream()
                .map(fila -> new ConteoDTO(fila[0].toString(), (Long) fila[1]))
                .collect(Collectors.toList());
    }

    // Conteo por género.
    public List<ConteoDTO> obtenerConteoPorGenero() {
        return libroRepository.contarPorGenero().stream()
                .map(fila -> new ConteoDTO((String) fila[0], (Long) fila[1]))
                .collect(Collectors.toList());
    }

    // Conteo por año de lectura.
    public List<ConteoDTO> obtenerConteoPorAnioLectura() {
        return libroRepository.contarPorAnioLectura().stream()
                .map(fila -> new ConteoDTO(fila[0].toString(), (Long) fila[1]))
                .collect(Collectors.toList());
    }

    // Ritmo de lectura: se calcula a partir del conteo por año, no es una query propia.
    public RitmoLecturaDTO obtenerRitmoLectura() {
        List<ConteoDTO> conteoPorAnio = obtenerConteoPorAnioLectura();

        long totalLibros = conteoPorAnio.stream()
                .mapToLong(ConteoDTO::getCantidad)
                .sum();

        int cantidadAnios = conteoPorAnio.size();

        Double promedio = cantidadAnios > 0
                ? (double) totalLibros / cantidadAnios
                : null;

        return new RitmoLecturaDTO(totalLibros, cantidadAnios, promedio);
    }
}