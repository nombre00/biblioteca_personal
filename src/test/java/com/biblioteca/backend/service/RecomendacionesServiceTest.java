package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.ConteoDobleDTO;
import com.biblioteca.backend.dto.SugerenciaLibroDTO;
import com.biblioteca.backend.model.Autor;
import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.model.Libro;
import com.biblioteca.backend.repository.LibroRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de RecomendacionesService. Nota importante: el service
 * usa java.util.Random instanciado inline (no inyectado), así que cuando
 * un autor tiene más de un libro pendiente, el resultado exacto no es
 * determinístico — los tests para ese caso solo verifican que el libro
 * elegido pertenece al conjunto de pendientes del autor, no cuál en
 * particular. Con un solo pendiente por autor, sí es determinístico
 * (Random.nextInt(1) siempre devuelve 0), y esos casos se usan para
 * aserciones más finas de orden y contenido.
 */
@ExtendWith(MockitoExtension.class)
class RecomendacionesServiceTest {

    @Mock
    private LibroRepository libroRepository;
    @Mock
    private EstadisticaService estadisticaService;

    private RecomendacionesService service;

    @BeforeEach
    void setUp() {
        service = new RecomendacionesService(libroRepository, estadisticaService);
    }

    // --- Helpers ---

    private ConteoDobleDTO autorEnRanking(Long autorId, String nombre, long leidos) {
        return new ConteoDobleDTO(nombre, null, leidos, autorId);
    }

    private Libro libroPendiente(Long id, Long autorId, String nombreAutor, String titulo) {
        Autor autor = new Autor();
        autor.setId(autorId);
        autor.setNombre(nombreAutor);
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitulo(titulo);
        libro.setPortadaUrl("http://ejemplo.com/" + id + ".jpg");
        libro.setEstado(EstadoLibro.POR_LEER);
        libro.setAutor(autor);
        return libro;
    }

    // --- Casos base ---

    @Nested
    class CasosBase {

        @Test
        void rankingVacio_devuelveListaVacia() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of());
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of());

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).isEmpty();
        }

        @Test
        void autorSinPendientes_seSaltaSinConsumirCupo() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Jenofonte", 10L)
            ));
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of());

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).isEmpty();
        }

        @Test
        void autorConUnPendiente_seEligeDeterministicamente() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Jenofonte", 10L)
            ));
            Libro pendiente = libroPendiente(100L, 1L, "Jenofonte", "Memorabilia");
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of(pendiente));

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isEqualTo(100L);
            assertThat(resultado.get(0).getTitulo()).isEqualTo("Memorabilia");
            assertThat(resultado.get(0).getAutorNombre()).isEqualTo("Jenofonte");
        }

        @Test
        void autorConVariosPendientes_eligeUnoDelConjunto() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Jenofonte", 10L)
            ));
            Libro pendiente1 = libroPendiente(100L, 1L, "Jenofonte", "Memorabilia");
            Libro pendiente2 = libroPendiente(101L, 1L, "Jenofonte", "Anábasis");
            Libro pendiente3 = libroPendiente(102L, 1L, "Jenofonte", "Ciropedia");
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER))
                    .thenReturn(List.of(pendiente1, pendiente2, pendiente3));

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isIn(100L, 101L, 102L);
        }
    }

    // --- Agrupación por autor ---

    @Nested
    class AgrupacionPorAutor {

        @Test
        void variosAutoresConUnPendienteCadaUno_respetaOrdenDelRanking() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Jenofonte", 15L),
                    autorEnRanking(2L, "Plauto", 9L),
                    autorEnRanking(3L, "Esquilo", 4L)
            ));
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of(
                    libroPendiente(100L, 1L, "Jenofonte", "Memorabilia"),
                    libroPendiente(200L, 2L, "Plauto", "Anfitrión"),
                    libroPendiente(300L, 3L, "Esquilo", "Los persas")
            ));

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).extracting(SugerenciaLibroDTO::getId)
                    .containsExactly(100L, 200L, 300L);
        }

        @Test
        void librosDeDistintosAutoresConMismoTitulo_seAgrupanPorAutorIdNoPorNombre() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Autor A", 5L),
                    autorEnRanking(2L, "Autor B", 3L)
            ));
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of(
                    libroPendiente(100L, 1L, "Autor A", "Título compartido"),
                    libroPendiente(200L, 2L, "Autor B", "Título compartido")
            ));

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(SugerenciaLibroDTO::getId)
                    .containsExactlyInAnyOrder(100L, 200L);
        }

        @Test
        void autorEnRankingSinLibrosPendientes_seSaltaYSiguienteAutorSeConsidera() {
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(List.of(
                    autorEnRanking(1L, "Sin pendientes", 20L),
                    autorEnRanking(2L, "Con pendiente", 5L)
            ));
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(List.of(
                    libroPendiente(200L, 2L, "Con pendiente", "Su único libro")
            ));

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isEqualTo(200L);
        }
    }

    // --- Cupo máximo de 21 sugerencias ---

    @Nested
    class CupoMaximo {

        @Test
        void respetaLimiteDe21SugerenciasSinConsumirCupoEnAutoresSinPendientes() {
            List<ConteoDobleDTO> ranking = new ArrayList<>();
            // 3 autores sin pendientes al inicio del ranking: no deben consumir cupo.
            ranking.add(autorEnRanking(1L, "Sin pendientes 1", 50L));
            ranking.add(autorEnRanking(2L, "Sin pendientes 2", 49L));
            ranking.add(autorEnRanking(3L, "Sin pendientes 3", 48L));
            // 22 autores con exactamente un pendiente cada uno: solo los primeros 21 deben incluirse.
            List<Libro> pendientes = new ArrayList<>();
            for (long i = 4; i <= 25; i++) {
                ranking.add(autorEnRanking(i, "Autor " + i, 50L - i));
                pendientes.add(libroPendiente(i * 100, i, "Autor " + i, "Libro de autor " + i));
            }
            when(estadisticaService.obtenerConteoPorAutor(null)).thenReturn(ranking);
            when(libroRepository.findByEstadoConAutor(EstadoLibro.POR_LEER)).thenReturn(pendientes);

            List<SugerenciaLibroDTO> resultado = service.obtenerPorAutorPendiente();

            assertThat(resultado).hasSize(21);
            // El autor 25 (el último, más allá del cupo) no debe estar incluido.
            assertThat(resultado).extracting(SugerenciaLibroDTO::getId)
                    .doesNotContain(2500L);
            // El autor 4 (primero con pendiente tras los 3 sin pendientes) sí debe estar.
            assertThat(resultado).extracting(SugerenciaLibroDTO::getId)
                    .contains(400L);
        }
    }
}