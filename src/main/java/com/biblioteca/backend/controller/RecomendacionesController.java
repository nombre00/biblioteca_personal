package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.SugerenciaLibroDTO;
import com.biblioteca.backend.service.RecomendacionesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionesController {

    private final RecomendacionesService recomendacionesService;

    public RecomendacionesController(RecomendacionesService recomendacionesService) {
        this.recomendacionesService = recomendacionesService;
    }

    @GetMapping("/por-autor-pendiente")
    public List<SugerenciaLibroDTO> obtenerPorAutorPendiente() {
        return recomendacionesService.obtenerPorAutorPendiente();
    }
}