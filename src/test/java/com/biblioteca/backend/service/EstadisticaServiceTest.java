package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.ConteoDTO;
import com.biblioteca.backend.dto.ConteoDobleDTO;
import com.biblioteca.backend.dto.RitmoLecturaDTO;
import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.repository.LibroRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de EstadisticaService. LibroRepository se mockea
 * devolviendo List<Object[]> tal como lo hacen las queries nativas/JPQL
 * reales, respetando el orden de columnas que cada método de
 * EstadisticaService asume al leer fila[0], fila[1], etc.
 */
@ExtendWith(MockitoExtension.class)
class EstadisticaServiceTest {

    @Mock
    private LibroRepository libroRepository;

    private EstadisticaService service;

    @BeforeEach
    void setUp() {
        service = new EstadisticaService(libroRepository);
    }

    // --- obtenerConteoPorEstado ---

    @Nested
    class ConteoPorEstado {

        @Test
        void mapeaFilasATConteoDTO() {
            when(libroRepository.contarPorEstado()).thenReturn(List.of(
                    new Object[]{EstadoLibro.LEIDO, 5L},
                    new Object[]{EstadoLibro.POR_LEER, 3L}
            ));

            List<ConteoDTO> resultado = service.obtenerConteoPorEstado();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getEtiqueta()).isEqualTo("LEIDO");
            assertThat(resultado.get(0).getCantidad()).isEqualTo(5L);
            assertThat(resultado.get(1).getEtiqueta()).isEqualTo("POR_LEER");
        }

        @Test
        void listaVacia_devuelveListaVacia() {
            when(libroRepository.contarPorEstado()).thenReturn(List.of());

            assertThat(service.obtenerConteoPorEstado()).isEmpty();
        }
    }

    // --- obtenerConteoPorAnioLectura ---

    @Nested
    class ConteoPorAnioLectura {

        @Test
        void mapeaFilasATConteoDTOSinOrdenar() {
            when(libroRepository.contarPorAnioLectura()).thenReturn(List.of(
                    new Object[]{2023, 4L},
                    new Object[]{2024, 9L}
            ));

            List<ConteoDTO> resultado = service.obtenerConteoPorAnioLectura();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getEtiqueta()).isEqualTo("2023");
            assertThat(resultado.get(0).getCantidad()).isEqualTo(4L);
            assertThat(resultado.get(1).getEtiqueta()).isEqualTo("2024");
        }
    }

    // --- obtenerConteoPorGenero ---

    @Nested
    class ConteoPorGenero {

        @Test
        void ordenaDescendentePorCantidad() {
            when(libroRepository.contarPorGenero()).thenReturn(List.of(
                    new Object[]{"Filosofía", 2L},
                    new Object[]{"Tragedia", 8L},
                    new Object[]{"Épica", 5L}
            ));

            List<ConteoDTO> resultado = service.obtenerConteoPorGenero();

            assertThat(resultado).extracting(ConteoDTO::getEtiqueta)
                    .containsExactly("Tragedia", "Épica", "Filosofía");
        }
    }

    // --- obtenerConteoPorGeneroPorAnio ---

    @Nested
    class ConteoPorGeneroPorAnio {

        @Test
        void propagaAnioAlRepositorioYOrdenaDescendente() {
            when(libroRepository.contarPorGeneroYAnio(2024)).thenReturn(List.of(
                    new Object[]{"Filosofía", 1L},
                    new Object[]{"Tragedia", 6L}
            ));

            List<ConteoDTO> resultado = service.obtenerConteoPorGeneroPorAnio(2024);

            verify(libroRepository).contarPorGeneroYAnio(2024);
            assertThat(resultado).extracting(ConteoDTO::getEtiqueta)
                    .containsExactly("Tragedia", "Filosofía");
        }
    }

    // --- obtenerRitmoLectura ---

    @Nested
    class RitmoLectura {

        @Test
        void calculaTotalCantidadAniosYPromedio() {
            when(libroRepository.contarPorAnioLectura()).thenReturn(List.of(
                    new Object[]{2023, 4L},
                    new Object[]{2024, 6L}
            ));

            RitmoLecturaDTO resultado = service.obtenerRitmoLectura();

            assertThat(resultado.getTotalLibrosConAnio()).isEqualTo(10L);
            assertThat(resultado.getCantidadAniosDistintos()).isEqualTo(2);
            assertThat(resultado.getPromedioLibrosPorAnio()).isEqualTo(5.0);
        }

        @Test
        void sinAniosConLectura_promedioEsNuloSinDivisionPorCero() {
            when(libroRepository.contarPorAnioLectura()).thenReturn(List.of());

            RitmoLecturaDTO resultado = service.obtenerRitmoLectura();

            assertThat(resultado.getTotalLibrosConAnio()).isEqualTo(0L);
            assertThat(resultado.getCantidadAniosDistintos()).isEqualTo(0);
            assertThat(resultado.getPromedioLibrosPorAnio()).isNull();
        }
    }

    // --- obtenerConteoPorAutor ---

    @Nested
    class ConteoPorAutor {

        @Test
        void conAnio_usaSoloContarPorAutorYAnio_totalQuedaNulo() {
            when(libroRepository.contarPorAutorYAnio(2024)).thenReturn(List.<Object[]>of(
                    new Object[]{"Jenofonte", 3L}
            ));

            List<ConteoDobleDTO> resultado = service.obtenerConteoPorAutor(2024);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEtiqueta()).isEqualTo("Jenofonte");
            assertThat(resultado.get(0).getCantidadTotal()).isNull();
            assertThat(resultado.get(0).getCantidadLeidos()).isEqualTo(3L);
            assertThat(resultado.get(0).getAutorId()).isNull();
        }

        @Test
        void sinAnio_combinaTotalesYLeidosPorAutorId() {
            when(libroRepository.contarTotalPorAutor()).thenReturn(List.of(
                    new Object[]{1L, "Jenofonte", 10L},
                    new Object[]{2L, "Plauto", 4L}
            ));
            when(libroRepository.contarLeidosPorAutor(EstadoLibro.LEIDO)).thenReturn(List.<Object[]>of(
                    new Object[]{1L, "Jenofonte", 7L}
                    // autor 2 (Plauto) no tiene fila -> debe defaultear a 0
            ));

            List<ConteoDobleDTO> resultado = service.obtenerConteoPorAutor(null);

            verify(libroRepository).contarLeidosPorAutor(EstadoLibro.LEIDO);
            assertThat(resultado).extracting(ConteoDobleDTO::getEtiqueta)
                    .containsExactly("Jenofonte", "Plauto"); // ordenado desc por leídos: 7, 0
            assertThat(resultado.get(0).getCantidadTotal()).isEqualTo(10L);
            assertThat(resultado.get(0).getCantidadLeidos()).isEqualTo(7L);
            assertThat(resultado.get(1).getCantidadTotal()).isEqualTo(4L);
            assertThat(resultado.get(1).getCantidadLeidos()).isEqualTo(0L);
            assertThat(resultado.get(1).getAutorId()).isEqualTo(2L);
        }
    }

    // --- obtenerConteoPorPais ---

    @Nested
    class ConteoPorPais {

        @Test
        void conAnio_usaSoloContarPorPaisYAnio_totalQuedaNulo() {
            when(libroRepository.contarPorPaisYAnio(2024)).thenReturn(List.<Object[]>of(
                    new Object[]{"Grecia", 5L}
            ));

            List<ConteoDobleDTO> resultado = service.obtenerConteoPorPais(2024);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEtiqueta()).isEqualTo("Grecia");
            assertThat(resultado.get(0).getCantidadTotal()).isNull();
            assertThat(resultado.get(0).getCantidadLeidos()).isEqualTo(5L);
        }

        @Test
        void sinAnio_combinaTotalesYLeidosPorNombreDePais_defaulteaAceroSiFalta() {
            when(libroRepository.contarTotalPorPais()).thenReturn(List.of(
                    new Object[]{"Grecia", 12L},
                    new Object[]{"Roma", 6L}
            ));
            when(libroRepository.contarLeidosPorPais(EstadoLibro.LEIDO)).thenReturn(List.<Object[]>of(
                    new Object[]{"Grecia", 8L}
                    // Roma no tiene fila -> debe defaultear a 0
            ));

            List<ConteoDobleDTO> resultado = service.obtenerConteoPorPais(null);

            verify(libroRepository).contarLeidosPorPais(EstadoLibro.LEIDO);
            assertThat(resultado).hasSize(2);
            ConteoDobleDTO grecia = resultado.stream()
                    .filter(c -> c.getEtiqueta().equals("Grecia")).findFirst().orElseThrow();
            ConteoDobleDTO roma = resultado.stream()
                    .filter(c -> c.getEtiqueta().equals("Roma")).findFirst().orElseThrow();
            assertThat(grecia.getCantidadTotal()).isEqualTo(12L);
            assertThat(grecia.getCantidadLeidos()).isEqualTo(8L);
            assertThat(roma.getCantidadTotal()).isEqualTo(6L);
            assertThat(roma.getCantidadLeidos()).isEqualTo(0L);
        }
    }
}