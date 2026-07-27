package com.biblioteca.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "libro")
@Getter
@Setter
@NoArgsConstructor
public class Libro {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "autor_id", nullable = false)
    private Autor autor;

    @Column(nullable = true)
    private String isbn;

    @Column(nullable = false)
    private String titulo;

    private String portadaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLibro estado;

    @ManyToMany
    @JoinTable(
        name = "libro_genero",
        joinColumns = @JoinColumn(name = "libro_id"),
        inverseJoinColumns = @JoinColumn(name = "genero_id")
    )
    private Set<Genero> generos = new HashSet<>();

    @Column(name = "anio_publicacion", nullable = true)
    private Integer anioPublicacion;

    @Column(name = "anio_lectura", nullable = true)
    private Integer anioLectura;

    @Column(name = "fecha_inicio", nullable = true)
    private LocalDate fechaInicio;

    @Column(name = "fecha_termino", nullable = true)
    private LocalDate fechaTermino;
}
