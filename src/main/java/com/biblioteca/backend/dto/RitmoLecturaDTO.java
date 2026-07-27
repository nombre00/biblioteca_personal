package com.biblioteca.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RitmoLecturaDTO {

    private Long totalLibrosConAnio;
    private Integer cantidadAniosDistintos;
    private Double promedioLibrosPorAnio;
}