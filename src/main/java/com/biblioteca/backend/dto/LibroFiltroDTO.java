package com.biblioteca.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LibroFiltroDTO {

    private String estado;
    private List<Long> generoIds;
    private Long paisAutorId;
    private String idiomaAutor;
    private String texto;
}