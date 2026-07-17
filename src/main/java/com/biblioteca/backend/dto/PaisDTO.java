package com.biblioteca.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaisDTO {

    private Long id;

    @NotBlank(message = "El nombre del país es obligatorio")
    private String nombre;
}