package com.biblioteca.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConteoDobleDTO {
    private String etiqueta;
    private Long cantidadTotal;   // null cuando hay filtro de año activo (no aplica)
    private Long cantidadLeidos;
}