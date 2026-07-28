package com.biblioteca.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SugerenciaLibroDTO {
    private Long libroId;
    private String titulo;
    private String autorNombre;
    private String urlPortada;
}
