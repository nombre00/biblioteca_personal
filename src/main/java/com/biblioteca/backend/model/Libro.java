package com.biblioteca.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String portadaURL;

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
}
