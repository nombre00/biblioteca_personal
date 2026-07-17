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
public class GeneroDTO {

    private Long id;

    @NotBlank(message = "El nombre del género es obligatorio")
    private String nombre;
}