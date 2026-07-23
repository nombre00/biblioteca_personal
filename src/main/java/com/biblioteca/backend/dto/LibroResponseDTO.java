package com.biblioteca.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LibroResponseDTO {

    private Long id;
    private String titulo;
    private String isbn;
    private String portadaUrl;
    private String estado;
    private AutorResumenDTO autor;
    private List<GeneroDTO> generos;
}