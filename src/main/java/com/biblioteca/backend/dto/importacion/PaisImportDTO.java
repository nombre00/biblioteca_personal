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

    // Se usa solo si paisId viene null: datos del país nuevo a crear
    // (o a buscar-o-crear, en el caso del placeholder PAIS_PENDIENTE).
    private PaisNuevoDTO datos;
}