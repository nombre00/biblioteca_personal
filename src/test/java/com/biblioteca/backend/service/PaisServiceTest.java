package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.PaisDTO;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.PaisRepository;

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
class PaisServiceTest {

    @Mock
    private PaisRepository paisRepository;

    private PaisService service;

    @BeforeEach
    void setUp() {
        service = new PaisService(paisRepository);
    }

    private Pais paisExistente(Long id, String nombre) {
        Pais pais = new Pais();
        pais.setId(id);
        pais.setNombre(nombre);
        return pais;
    }

    // --- listarTodos ---

    @Nested
    class ListarTodos {

        @Test
        void listaVacia_devuelveListaVacia() {
            when(paisRepository.findAll()).thenReturn(List.of());

            assertThat(service.listarTodos()).isEmpty();
        }

        @Test
        void devuelveTodosLosPaisesConvertidosADto() {
            when(paisRepository.findAll()).thenReturn(List.of(
                    paisExistente(1L, "Grecia"),
                    paisExistente(2L, "Roma")
            ));

            List<PaisDTO> resultado = service.listarTodos();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Grecia");
            assertThat(resultado.get(1).getNombre()).isEqualTo("Roma");
        }
    }

    // --- crear ---

    @Nested
    class Crear {

        @Test
        void nombreDuplicado_lanzaExcepcionSinGuardar() {
            PaisDTO dto = new PaisDTO(null, "Grecia");
            when(paisRepository.findByNombreIgnoreCase("Grecia"))
                    .thenReturn(Optional.of(paisExistente(1L, "Grecia")));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoDuplicadoException.class);

            verify(paisRepository, never()).save(any());
        }

        @Test
        void nombreLibre_creaYDevuelveDto() {
            PaisDTO dto = new PaisDTO(null, "Esparta");
            when(paisRepository.findByNombreIgnoreCase("Esparta")).thenReturn(Optional.empty());
            when(paisRepository.save(any(Pais.class))).thenReturn(paisExistente(9L, "Esparta"));

            PaisDTO resultado = service.crear(dto);

            ArgumentCaptor<Pais> captor = ArgumentCaptor.forClass(Pais.class);
            verify(paisRepository).save(captor.capture());
            assertThat(captor.getValue().getNombre()).isEqualTo("Esparta");

            assertThat(resultado.getId()).isEqualTo(9L);
            assertThat(resultado.getNombre()).isEqualTo("Esparta");
        }

        @Test
        void nombreDuplicadoConDistintasMayusculas_seDetectaIgual() {
            // findByNombreIgnoreCase ya se encarga de la comparación case-insensitive
            // en el repositorio; el service solo debe delegar el nombre tal cual vino.
            PaisDTO dto = new PaisDTO(null, "grecia");
            when(paisRepository.findByNombreIgnoreCase("grecia"))
                    .thenReturn(Optional.of(paisExistente(1L, "Grecia")));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoDuplicadoException.class);
        }
    }

    // --- eliminar ---

    @Nested
    class Eliminar {

        @Test
        void paisInexistente_lanzaExcepcionSinLlamarDelete() {
            when(paisRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(paisRepository, never()).delete(any());
        }

        @Test
        void paisExistente_seElimina() {
            Pais existente = paisExistente(1L, "Grecia");
            when(paisRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            verify(paisRepository).delete(existente);
        }
    }
}