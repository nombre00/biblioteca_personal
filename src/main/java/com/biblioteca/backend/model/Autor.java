package com.biblioteca.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Table(name = "autor")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Autor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String idioma;

    @ManyToOne
    @JoinColumn(name = "pais_id")
    private Pais pais;

    @Column(name = "retrato_url", nullable = true)
    private String retratoUrl;

    @Column(name = "fecha_nacimiento", nullable = true)
    private LocalDate fechaNacimiento;

    @Column(name = "anio_nacimiento_aprox", nullable = true)
    private Integer anioNacimientoAprox;

    @Column(name = "fecha_defuncion", nullable = true)
    private LocalDate fechaDefuncion;

    @Column(name = "anio_defuncion_aprox", nullable = true)
    private Integer anioDefuncionAprox;
}
