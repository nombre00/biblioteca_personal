package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.ConteoDTO;
import com.biblioteca.backend.dto.RitmoLecturaDTO;
import com.biblioteca.backend.service.EstadisticaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/por-estado")
    public ResponseEntity<List<ConteoDTO>> obtenerConteoPorEstado() {
        return ResponseEntity.ok(estadisticaService.obtenerConteoPorEstado());
    }

    @GetMapping("/por-genero")
    public ResponseEntity<List<ConteoDTO>> obtenerConteoPorGenero() {
        return ResponseEntity.ok(estadisticaService.obtenerConteoPorGenero());
    }

    @GetMapping("/por-anio-lectura")
    public ResponseEntity<List<ConteoDTO>> obtenerConteoPorAnioLectura() {
        return ResponseEntity.ok(estadisticaService.obtenerConteoPorAnioLectura());
    }

    @GetMapping("/ritmo-lectura")
    public ResponseEntity<RitmoLecturaDTO> obtenerRitmoLectura() {
        return ResponseEntity.ok(estadisticaService.obtenerRitmoLectura());
    }
}