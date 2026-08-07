package com.biblioteca.backend.dto.importacion;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AutorNuevoDTO {

    private String nombre;
    private String idioma;

    // Null si Wikidata no encontró país para el autor -> resuelve a
    // PAIS_PENDIENTE en el service. Si viene, puede traer paisId
    // (match encontrado) o solo nombre (país nuevo a crear).
    private PaisImportDTO pais;

    private String retratoUrl;
    private LocalDate fechaNacimiento;
    private Integer anioNacimientoAprox;
    private LocalDate fechaDefuncion;
    private Integer anioDefuncionAprox;
}