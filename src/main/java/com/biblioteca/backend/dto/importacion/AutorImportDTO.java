package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class AutorImportDTO {

    // Uno de los dos, nunca ambos ni ninguno (se valida en el service).
    private Long autorId;
    private AutorNuevoDTO datos;
}