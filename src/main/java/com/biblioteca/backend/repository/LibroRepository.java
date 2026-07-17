package com.biblioteca.backend.repository;

import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    // Filtrar por estado (POR_LEER, LEYENDO, LEIDO)
    List<Libro> findByEstado(EstadoLibro estado);

    // Buscar por título (para el buscador)
    List<Libro> findByTituloContainingIgnoreCase(String titulo);

    // Buscar por nacionalidad del autor (lo que pediste al inicio)
    List<Libro> findByAutor_Pais_NombreIgnoreCase(String nombrePais);

    // Buscar por autor
    List<Libro> findByAutor_Id(Long autorId);

    // Buscar por ISBN (para validar duplicados antes de insertar)
    Optional<Libro> findByIsbn(String isbn);

    // Buscar por género (necesita @Query porque es relación ManyToMany)
    @Query("SELECT l FROM Libro l JOIN l.generos g WHERE LOWER(g.nombre) = LOWER(:nombreGenero)")
    List<Libro> findByGeneroNombre(@Param("nombreGenero") String nombreGenero);
}