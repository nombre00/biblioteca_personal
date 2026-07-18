package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.PaisDTO;
import com.biblioteca.backend.service.PaisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paises")
public class PaisController {

    private final PaisService paisService;

    public PaisController(PaisService paisService) {
        this.paisService = paisService;
    }

    @GetMapping
    public ResponseEntity<List<PaisDTO>> listarTodos() {
        return ResponseEntity.ok(paisService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<PaisDTO> crear(@Valid @RequestBody PaisDTO dto) {
        PaisDTO creado = paisService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        paisService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}