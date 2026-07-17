package com.biblioteca.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorResumenDTO {

    private Long id;
    private String nombre;
    private String idioma;
    private String paisNombre;
}