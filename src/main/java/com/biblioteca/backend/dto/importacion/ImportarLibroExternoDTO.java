package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ImportarLibroExternoDTO {

    private String titulo;
    private Integer anioPublicacion;
    private String isbn;
    private String portadaUrl;

    // Puede venir null desde agentes-ia si algún día cambia el default;
    // se resuelve a "POR_LEER" en el service si así es.
    private String estado;

    private AutorImportDTO autor;
    private List<GeneroImportDTO> generos;
}