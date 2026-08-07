package com.biblioteca.backend.controller;

import com.biblioteca.backend.dto.LibroResponseDTO;
import com.biblioteca.backend.dto.importacion.ImportarLibroExternoDTO;
import com.biblioteca.backend.service.LibroImportacionesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/libros")
public class LibroImportacionesController {

    private final LibroImportacionesService libroImportacionesService;

    public LibroImportacionesController(LibroImportacionesService libroImportacionesService) {
        this.libroImportacionesService = libroImportacionesService;
    }

    @PostMapping("/importar-externo")
    public ResponseEntity<LibroResponseDTO> importarExterno(@RequestBody ImportarLibroExternoDTO dto) {
        LibroResponseDTO creado = libroImportacionesService.importarLibroExterno(dto);
        return ResponseEntity.ok(creado);
    }
}