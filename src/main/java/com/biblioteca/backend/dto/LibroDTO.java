package com.biblioteca.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LibroDTO {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String isbn;

    private String portadaUrl;

    @NotNull(message = "El estado es obligatorio")
    private String estado;

    @NotNull(message = "El autor es obligatorio")
    private Long autorId;

    private List<Long> generoIds;
}