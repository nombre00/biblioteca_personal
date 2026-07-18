package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.LibroDTO;
import com.biblioteca.backend.dto.LibroFiltroDTO;
import com.biblioteca.backend.dto.LibroResponseDTO;
import com.biblioteca.backend.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @GetMapping
    public ResponseEntity<List<LibroResponseDTO>> listarTodos() {
        return ResponseEntity.ok(libroService.listarTodos());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<LibroResponseDTO>> buscarConFiltros(LibroFiltroDTO filtro) {
        return ResponseEntity.ok(libroService.buscarConFiltros(filtro));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(libroService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<LibroResponseDTO> crear(@Valid @RequestBody LibroDTO dto) {
        LibroResponseDTO creado = libroService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibroResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody LibroDTO dto) {
        return ResponseEntity.ok(libroService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}