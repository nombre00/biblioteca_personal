package com.biblioteca.backend.repository;

import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.model.Libro;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class LibroSpecification {

    // Constructor privado: esta clase solo tiene métodos estáticos, no se instancia
    private LibroSpecification() {
    }

    // Los métodos abajo revisan si se ingresó el parámetro de búsqueda, cuando no retorna un nulo, cuando si retorna la respuesta.

    public static Specification<Libro> tieneEstado(String estado) {
        return (root, query, cb) -> {
            // Si el estado es nulo o está en blanco retorna nulo.
            if (estado == null || estado.isBlank()) return null;
            // Si no, retorna el resultado de la bbúsqueda.
            return cb.equal(root.get("estado"), estado);
        };
    }

    public static Specification<Libro> tieneAlgunGenero(List<Long> generoIds) {
        return (root, query, cb) -> {
            // Si el estado es nulo o está en blanco retorna nulo.
            if (generoIds == null || generoIds.isEmpty()) return null;
            // Permite buscar por más de un género.
            query.distinct(true); // evita duplicados por el join ManyToMany
            Join<Libro, Genero> generos = root.join("generos");
            // Retorna el resultado de la bbúsqueda.
            return generos.get("id").in(generoIds);
        };
    }

    public static Specification<Libro> tienePaisAutor(Long paisId) {
        return (root, query, cb) -> {
            // Si el estado es nulo o está en blanco retorna nulo.
            if (paisId == null) return null;
            // Si no, retorna el resultado de la bbúsqueda.
            return cb.equal(root.get("autor").get("pais").get("id"), paisId);
        };
    }

    public static Specification<Libro> tieneIdiomaAutor(String idioma) {
        return (root, query, cb) -> {
            // Si el estado es nulo o está en blanco retorna nulo.
            if (idioma == null || idioma.isBlank()) return null;
            // Si no, retorna el resultado de la bbúsqueda.
            return cb.equal(cb.lower(root.get("autor").get("idioma")), idioma.toLowerCase());
        };
    }

    // Permite buscar usando textos incompletos por título del libro o nombre del autor.
    public static Specification<Libro> textoLibre(String texto) {
        return (root, query, cb) -> {
            // Si el estado es nulo o está en blanco retorna nulo.
            if (texto == null || texto.isBlank()) return null;
            // Revisa lo que hay de texto de forma no específica, no busca un match, solo que incluya el texto hasta donde está.
            String like = "%" + texto.toLowerCase() + "%";
            // Rretorna el resultado de la bbúsqueda.
            return cb.or(
                cb.like(cb.lower(root.get("titulo")), like),
                cb.like(cb.lower(root.get("autor").get("nombre")), like)
            );
        };
    }
}