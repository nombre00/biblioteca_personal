package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PaisImportDTO {

    // Si viene, se vincula directo al país existente con este id.
    private Long paisId;

    // Se usa solo si paisId viene null: nombre normalizado por el LLM
    // (o ausente del todo si Wikidata no encontró país para el autor).
    private String nombre;
}