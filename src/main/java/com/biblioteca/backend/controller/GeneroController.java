package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.GeneroDTO;
import com.biblioteca.backend.service.GeneroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/generos")
public class GeneroController {

    private final GeneroService generoService;

    public GeneroController(GeneroService generoService) {
        this.generoService = generoService;
    }

    @GetMapping
    public ResponseEntity<List<GeneroDTO>> listarTodos() {
        return ResponseEntity.ok(generoService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<GeneroDTO> crear(@Valid @RequestBody GeneroDTO dto) {
        GeneroDTO creado = generoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        generoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
