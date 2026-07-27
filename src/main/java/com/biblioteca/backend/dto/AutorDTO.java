package com.biblioteca.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AutorDTO {

    @NotBlank(message = "El nombre del autor es obligatorio")
    private String nombre;

    private String idioma;

    @NotNull(message = "El país es obligatorio")
    private Long paisId;

    private String retratoUrl;

    private LocalDate fechaNacimiento;

    private Integer anioNacimientoAprox;

    private LocalDate fechaDefuncion;

    private Integer anioDefuncionAprox;
}