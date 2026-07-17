package com.biblioteca.backend.repository;

import com.biblioteca.backend.model.Pais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaisRepository extends JpaRepository<Pais, Long> {

    Optional<Pais> findByNombreIgnoreCase(String nombre);
}