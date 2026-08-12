package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportarLibroExternoDTO {

    private String titulo;
    private Integer anioPublicacion;
    private String isbn;
    private String portadaUrl;

    // Puede venir null desde agentes-ia si algún día cambia el default;
    // se resuelve a "POR_LEER" en el service si así es.
    private String estado;

    // Lectura personal — opcionales, el usuario los llena en
    // confirmar-importar solo si ya leyó el libro que está importando.
    // Mismo shape que LibroDTO (el que usa el CRUD normal de libro-form).
    private Integer anioLectura;
    private LocalDate fechaInicio;
    private LocalDate fechaTermino;

    private AutorImportDTO autor;
    private List<GeneroImportDTO> generos;
}