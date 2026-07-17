package com.biblioteca.backend.repository;

import com.biblioteca.backend.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    // Buscar autores por nacionalidad (nombre del país)
    List<Autor> findByPais_NombreIgnoreCase(String nombrePais);

    // Buscar autor por nombre (para evitar duplicados al agregar)
    List<Autor> findByNombreContainingIgnoreCase(String nombre);
}