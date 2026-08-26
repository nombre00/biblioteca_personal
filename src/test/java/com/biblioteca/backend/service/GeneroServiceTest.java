package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.GeneroDTO;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.repository.GeneroRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneroServiceTest {

    @Mock
    private GeneroRepository generoRepository;

    private GeneroService service;

    @BeforeEach
    void setUp() {
        service = new GeneroService(generoRepository);
    }

    // --- Helpers ---

    private Genero generoExistente(Long id, String nombre, String iconoSlug) {
        Genero genero = new Genero();
        genero.setId(id);
        genero.setNombre(nombre);
        genero.setIconoSlug(iconoSlug);
        return genero;
    }

    // --- listarTodos ---

    @Nested
    class ListarTodos {

        @Test
        void listaVacia_devuelveListaVacia() {
            when(generoRepository.findAll()).thenReturn(List.of());

            assertThat(service.listarTodos()).isEmpty();
        }

        @Test
        void devuelveTodosLosGenerosConvertidosADto() {
            when(generoRepository.findAll()).thenReturn(List.of(
                    generoExistente(1L, "Tragedia", "tragedia.png"),
                    generoExistente(2L, "Épica", "epica.png")
            ));

            List<GeneroDTO> resultado = service.listarTodos();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Tragedia");
            assertThat(resultado.get(1).getNombre()).isEqualTo("Épica");
        }
    }

    // --- crear ---

    @Nested
    class Crear {

        @Test
        void nombreDuplicado_lanzaExcepcionSinGuardar() {
            GeneroDTO dto = new GeneroDTO(null, "Tragedia", "tragedia.png");
            when(generoRepository.findByNombreIgnoreCase("Tragedia"))
                    .thenReturn(Optional.of(generoExistente(1L, "Tragedia", "otro.png")));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoDuplicadoException.class);

            verify(generoRepository, never()).save(any());
        }

        @Test
        void nombreLibre_creaYDevuelveDto() {
            GeneroDTO dto = new GeneroDTO(null, "Filosofía", "filosofia.png");
            when(generoRepository.findByNombreIgnoreCase("Filosofía")).thenReturn(Optional.empty());
            when(generoRepository.save(any(Genero.class)))
                    .thenReturn(generoExistente(5L, "Filosofía", "filosofia.png"));

            GeneroDTO resultado = service.crear(dto);

            ArgumentCaptor<Genero> captor = ArgumentCaptor.forClass(Genero.class);
            verify(generoRepository).save(captor.capture());
            assertThat(captor.getValue().getNombre()).isEqualTo("Filosofía");
            assertThat(captor.getValue().getIconoSlug()).isEqualTo("filosofia.png");

            assertThat(resultado.getId()).isEqualTo(5L);
            assertThat(resultado.getNombre()).isEqualTo("Filosofía");
        }
    }

    // --- actualizar ---

    @Nested
    class Actualizar {

        @Test
        void generoInexistente_lanzaExcepcionSinConsultarDuplicado() {
            GeneroDTO dto = new GeneroDTO(null, "Tragedia", "tragedia.png");
            when(generoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizar(99L, dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(generoRepository, never()).findByNombreIgnoreCase(any());
            verify(generoRepository, never()).save(any());
        }

        @Test
        void nombreDuplicadoPerteneceAOtroGenero_lanzaExcepcion() {
            Genero existente = generoExistente(1L, "Tragedia", "tragedia.png");
            GeneroDTO dto = new GeneroDTO(null, "Épica", "epica.png");
            when(generoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(generoRepository.findByNombreIgnoreCase("Épica"))
                    .thenReturn(Optional.of(generoExistente(2L, "Épica", "otro.png")));

            assertThatThrownBy(() -> service.actualizar(1L, dto))
                    .isInstanceOf(RecursoDuplicadoException.class);

            verify(generoRepository, never()).save(any());
        }

        @Test
        void renombrarAlMismoNombrePropio_noLanzaExcepcion() {
            // El género encontrado por nombre es el mismo que se está editando
            // (mismo id) -> el filtro lo excluye, no debe considerarse duplicado.
            Genero existente = generoExistente(1L, "Tragedia", "tragedia.png");
            GeneroDTO dto = new GeneroDTO(null, "Tragedia", "tragedia-nuevo-icono.png");
            when(generoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(generoRepository.findByNombreIgnoreCase("Tragedia")).thenReturn(Optional.of(existente));
            when(generoRepository.save(any(Genero.class))).thenReturn(existente);

            GeneroDTO resultado = service.actualizar(1L, dto);

            verify(generoRepository).save(existente);
            assertThat(resultado.getIconoSlug()).isEqualTo("tragedia-nuevo-icono.png");
        }

        @Test
        void datosValidos_actualizaEntidadExistenteYDevuelveDto() {
            Genero existente = generoExistente(1L, "Tragedia", "tragedia.png");
            GeneroDTO dto = new GeneroDTO(null, "Tragedia griega", "tragedia-griega.png");
            when(generoRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(generoRepository.findByNombreIgnoreCase("Tragedia griega")).thenReturn(Optional.empty());
            when(generoRepository.save(any(Genero.class))).thenReturn(existente);

            GeneroDTO resultado = service.actualizar(1L, dto);

            assertThat(existente.getNombre()).isEqualTo("Tragedia griega");
            assertThat(existente.getIconoSlug()).isEqualTo("tragedia-griega.png");
            verify(generoRepository).save(existente);
            assertThat(resultado.getId()).isEqualTo(1L);
        }
    }

    // --- eliminar ---

    @Nested
    class Eliminar {

        @Test
        void generoInexistente_lanzaExcepcionSinLlamarDelete() {
            when(generoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(generoRepository, never()).delete(any());
        }

        @Test
        void generoExistente_seElimina() {
            Genero existente = generoExistente(1L, "Tragedia", "tragedia.png");
            when(generoRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            verify(generoRepository).delete(existente);
        }
    }
}