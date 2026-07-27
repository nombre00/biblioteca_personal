package com.biblioteca.backend.repository;

import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long>, 
                                          JpaSpecificationExecutor<Libro>{

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

    // JpaSpecificationExecutor agrega automáticamente:
    // findAll(Specification<Libro> spec)
    // findAll(Specification<Libro> spec, Pageable pageable)
    // findOne(Specification<Libro> spec)
    // count(Specification<Libro> spec)

    

    // --- Estadísticas ---

    // Conteo de libros agrupados por estado.
    @Query("SELECT l.estado, COUNT(l) FROM Libro l GROUP BY l.estado")
    List<Object[]> contarPorEstado();

    // Conteo de libros agrupados por género (join por ser ManyToMany).
    @Query("SELECT g.nombre, COUNT(l) FROM Libro l JOIN l.generos g GROUP BY g.nombre")
    List<Object[]> contarPorGenero();

    // Conteo de libros agrupados por año de lectura (excluye libros sin año registrado).
    @Query("SELECT l.anioLectura, COUNT(l) FROM Libro l WHERE l.anioLectura IS NOT NULL " +
           "GROUP BY l.anioLectura ORDER BY l.anioLectura")
    List<Object[]> contarPorAnioLectura();
}