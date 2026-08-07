package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaisNuevoDTO {

    // Nombre normalizado por el LLM, o el placeholder PAIS_PENDIENTE
    // cuando Wikidata no encontró país para el autor.
    private String nombre;
}