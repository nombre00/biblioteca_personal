package com.biblioteca.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorResponseDTO {

    private Long id;
    private String nombre;
    private String idioma;
    private PaisDTO pais;
    private String retratoUrl;
    private LocalDate fechaNacimiento;
    private Integer anioNacimientoAprox;
    private LocalDate fechaDefuncion;
    private Integer anioDefuncionAprox;
}