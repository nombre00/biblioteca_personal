package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.LibroDTO;
import com.biblioteca.backend.dto.LibroFiltroDTO;
import com.biblioteca.backend.dto.LibroResponseDTO;
import com.biblioteca.backend.exception.DatosInvalidosException;
import com.biblioteca.backend.exception.RecursoDuplicadoException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Autor;
import com.biblioteca.backend.model.EstadoLibro;
import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.model.Libro;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.AutorRepository;
import com.biblioteca.backend.repository.GeneroRepository;
import com.biblioteca.backend.repository.LibroRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepository;
    @Mock
    private AutorRepository autorRepository;
    @Mock
    private GeneroRepository generoRepository;

    private LibroService service;

    @BeforeEach
    void setUp() {
        service = new LibroService(libroRepository, autorRepository, generoRepository);
    }

    // --- Helpers ---

    private Pais pais(Long id, String nombre) {
        Pais pais = new Pais();
        pais.setId(id);
        pais.setNombre(nombre);
        return pais;
    }

    private Autor autor(Long id, Pais pais) {
        Autor autor = new Autor();
        autor.setId(id);
        autor.setNombre("Jenofonte");
        autor.setIdioma("griego antiguo");
        autor.setPais(pais);
        return autor;
    }

    private Genero genero(Long id, String nombre) {
        Genero genero = new Genero();
        genero.setId(id);
        genero.setNombre(nombre);
        genero.setIconoSlug(nombre.toLowerCase() + ".png");
        return genero;
    }

    private Libro libroExistente(Long id, String isbn, Autor autor, Set<Genero> generos) {
        Libro libro = new Libro();
        libro.setId(id);
        libro.setTitulo("Memorabilia");
        libro.setIsbn(isbn);
        libro.setPortadaUrl("http://ejemplo.com/portada.jpg");
        libro.setEstado(EstadoLibro.POR_LEER);
        libro.setAutor(autor);
        libro.setGeneros(generos);
        libro.setAnioPublicacion(1998);
        return libro;
    }

    private LibroDTO dtoValido(Long autorId) {
        LibroDTO dto = new LibroDTO();
        dto.setTitulo("Memorabilia");
        dto.setIsbn("978-84-376-0494-7");
        dto.setPortadaUrl("http://ejemplo.com/portada.jpg");
        dto.setEstado("POR_LEER");
        dto.setAutorId(autorId);
        dto.setAnioPublicacion(1998);
        return dto;
    }

    // --- listarTodos / listarPorAutor ---

    @Nested
    class Listados {

        @Test
        void listarTodos_mapeaLibroConPaisYGenerosCorrectamente() {
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro libro = libroExistente(1L, "978-1", autor, Set.of(genero(5L, "Filosofía")));
            when(libroRepository.findAll()).thenReturn(List.of(libro));

            List<LibroResponseDTO> resultado = service.listarTodos();

            assertThat(resultado).hasSize(1);
            LibroResponseDTO dto = resultado.get(0);
            assertThat(dto.getAutor().getPaisNombre()).isEqualTo("Grecia");
            assertThat(dto.getGeneros()).extracting("nombre").containsExactly("Filosofía");
            assertThat(dto.getEstado()).isEqualTo("POR_LEER");
        }

        @Test
        void listarTodos_autorSinPais_paisQuedaNuloEnDto() {
            Autor autorSinPais = autor(1L, null);
            Libro libro = libroExistente(1L, "978-1", autorSinPais, Set.of());
            when(libroRepository.findAll()).thenReturn(List.of(libro));

            List<LibroResponseDTO> resultado = service.listarTodos();

            assertThat(resultado.get(0).getAutor().getPaisNombre()).isNull();
        }

        @Test
        void listarPorAutor_delegaAlRepositorioPorAutorId() {
            Autor autor = autor(7L, pais(10L, "Grecia"));
            Libro libro = libroExistente(1L, "978-1", autor, Set.of());
            when(libroRepository.findByAutor_Id(7L)).thenReturn(List.of(libro));

            List<LibroResponseDTO> resultado = service.listarPorAutor(7L);

            assertThat(resultado).hasSize(1);
            verify(libroRepository).findByAutor_Id(7L);
        }
    }

    // --- buscarConFiltros ---
    // Solo se verifica que el service arma una Specification y delega al
    // repositorio, y que mapea bien el resultado. La lógica real de cada
    // predicado (LibroSpecification) no es verificable con un mock puro
    // del repositorio; requeriría un @DataJpaTest con H2 para probarse
    // contra SQL generado de verdad.

    @Nested
    class BuscarConFiltros {

        @Test
        void delegaAlRepositorioConSpecificationYMapeaResultado() {
            LibroFiltroDTO filtro = new LibroFiltroDTO();
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro libro = libroExistente(1L, "978-1", autor, Set.of());
            when(libroRepository.findAll(ArgumentMatchers.<Specification<Libro>>any())).thenReturn(List.of(libro));

            List<LibroResponseDTO> resultado = service.buscarConFiltros(filtro);

            assertThat(resultado).hasSize(1);
            verify(libroRepository).findAll(ArgumentMatchers.<Specification<Libro>>any());
        }
    }

    // --- buscarPorId ---

    @Nested
    class BuscarPorId {

        @Test
        void devuelveDtoSiExiste() {
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro libro = libroExistente(1L, "978-1", autor, Set.of());
            when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));

            LibroResponseDTO resultado = service.buscarPorId(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
        }

        @Test
        void lanzaExcepcionSiNoExiste() {
            when(libroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // --- crear ---

    @Nested
    class Crear {

        @Test
        void isbnDuplicado_lanzaExcepcionSinConsultarAutor() {
            LibroDTO dto = dtoValido(1L);
            when(libroRepository.findByIsbn(dto.getIsbn()))
                    .thenReturn(Optional.of(libroExistente(2L, dto.getIsbn(), autor(1L, null), Set.of())));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoDuplicadoException.class);

            verify(autorRepository, never()).findById(any());
            verify(libroRepository, never()).save(any());
        }

        @Test
        void isbnNulo_noConsultaDuplicado() {
            LibroDTO dto = dtoValido(1L);
            dto.setIsbn(null);
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));
            when(libroRepository.save(any(Libro.class)))
                    .thenReturn(libroExistente(1L, null, autor(1L, pais(10L, "Grecia")), Set.of()));

            service.crear(dto);

            verify(libroRepository, never()).findByIsbn(any());
        }

        @Test
        void isbnEnBlanco_noConsultaDuplicado() {
            LibroDTO dto = dtoValido(1L);
            dto.setIsbn("   ");
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));
            when(libroRepository.save(any(Libro.class)))
                    .thenReturn(libroExistente(1L, "   ", autor(1L, pais(10L, "Grecia")), Set.of()));

            service.crear(dto);

            verify(libroRepository, never()).findByIsbn(any());
        }

        @Test
        void autorInexistente_lanzaExcepcionSinGuardar() {
            LibroDTO dto = dtoValido(99L);
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(libroRepository, never()).save(any());
        }

        @Test
        void fechaInicioPosteriorAFechaTermino_lanzaExcepcion() {
            LibroDTO dto = dtoValido(1L);
            dto.setFechaInicio(LocalDate.of(2024, 2, 1));
            dto.setFechaTermino(LocalDate.of(2024, 1, 1));
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verify(libroRepository, never()).save(any());
        }

        @Test
        void fechaInicioIgualAFechaTermino_esValido() {
            LibroDTO dto = dtoValido(1L);
            LocalDate mismaFecha = LocalDate.of(2024, 1, 1);
            dto.setFechaInicio(mismaFecha);
            dto.setFechaTermino(mismaFecha);
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));
            when(libroRepository.save(any(Libro.class)))
                    .thenReturn(libroExistente(1L, dto.getIsbn(), autor(1L, pais(10L, "Grecia")), Set.of()));

            service.crear(dto);

            verify(libroRepository).save(any(Libro.class));
        }

        @Test
        void estadoInvalido_lanzaExcepcion() {
            LibroDTO dto = dtoValido(1L);
            dto.setEstado("NO_EXISTE");
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(libroRepository, never()).save(any());
        }

        @Test
        void sinGeneroIds_asignaConjuntoVacioSinConsultarGeneroRepository() {
            LibroDTO dto = dtoValido(1L);
            dto.setGeneroIds(null);
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));
            when(libroRepository.save(any(Libro.class)))
                    .thenReturn(libroExistente(1L, dto.getIsbn(), autor(1L, pais(10L, "Grecia")), Set.of()));

            service.crear(dto);

            verify(generoRepository, never()).findAllById(any());
            ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
            verify(libroRepository).save(captor.capture());
            assertThat(captor.getValue().getGeneros()).isEmpty();
        }

        @Test
        void conGeneroIds_asignaGenerosEncontrados() {
            LibroDTO dto = dtoValido(1L);
            dto.setGeneroIds(List.of(5L, 6L));
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor(1L, pais(10L, "Grecia"))));
            when(generoRepository.findAllById(List.of(5L, 6L)))
                    .thenReturn(List.of(genero(5L, "Filosofía"), genero(6L, "Tragedia")));
            when(libroRepository.save(any(Libro.class)))
                    .thenReturn(libroExistente(1L, dto.getIsbn(), autor(1L, pais(10L, "Grecia")), Set.of()));

            service.crear(dto);

            ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
            verify(libroRepository).save(captor.capture());
            assertThat(captor.getValue().getGeneros()).hasSize(2);
        }

        @Test
        void datosValidos_guardaYDevuelveDtoCorrecto() {
            LibroDTO dto = dtoValido(1L);
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.empty());
            Autor autor = autor(1L, pais(10L, "Grecia"));
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
            Libro guardado = libroExistente(1L, dto.getIsbn(), autor, Set.of());
            when(libroRepository.save(any(Libro.class))).thenReturn(guardado);

            LibroResponseDTO resultado = service.crear(dto);

            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getTitulo()).isEqualTo("Memorabilia");
        }
    }

    // --- actualizar ---

    @Nested
    class Actualizar {

        @Test
        void libroInexistente_lanzaExcepcionSinConsultarIsbn() {
            LibroDTO dto = dtoValido(1L);
            when(libroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizar(99L, dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(libroRepository, never()).findByIsbn(any());
            verify(libroRepository, never()).save(any());
        }

        @Test
        void isbnDuplicadoPerteneceAOtroLibro_lanzaExcepcion() {
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro existente = libroExistente(1L, "978-1", autor, Set.of());
            LibroDTO dto = dtoValido(1L);
            dto.setIsbn("978-2");
            when(libroRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(libroRepository.findByIsbn("978-2"))
                    .thenReturn(Optional.of(libroExistente(2L, "978-2", autor, Set.of())));

            assertThatThrownBy(() -> service.actualizar(1L, dto))
                    .isInstanceOf(RecursoDuplicadoException.class);

            verify(libroRepository, never()).save(any());
        }

        @Test
        void isbnDuplicadoPerteneceAlMismoLibro_noLanzaExcepcion() {
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro existente = libroExistente(1L, "978-1", autor, Set.of());
            LibroDTO dto = dtoValido(1L);
            dto.setIsbn("978-1");
            when(libroRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(libroRepository.findByIsbn("978-1")).thenReturn(Optional.of(existente));
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
            when(libroRepository.save(any(Libro.class))).thenReturn(existente);

            service.actualizar(1L, dto);

            verify(libroRepository).save(existente);
        }

        @Test
        void datosValidos_actualizaEntidadExistenteYDevuelveDto() {
            Autor autor = autor(1L, pais(10L, "Grecia"));
            Libro existente = libroExistente(1L, "978-1", autor, Set.of());
            LibroDTO dto = dtoValido(1L);
            dto.setTitulo("Memorabilia (edición revisada)");
            when(libroRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(libroRepository.findByIsbn(dto.getIsbn())).thenReturn(Optional.of(existente));
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autor));
            when(libroRepository.save(any(Libro.class))).thenReturn(existente);

            LibroResponseDTO resultado = service.actualizar(1L, dto);

            assertThat(existente.getTitulo()).isEqualTo("Memorabilia (edición revisada)");
            verify(libroRepository).save(existente);
            assertThat(resultado.getId()).isEqualTo(1L);
        }
    }

    // --- eliminar ---

    @Nested
    class Eliminar {

        @Test
        void libroInexistente_lanzaExcepcionSinLlamarDelete() {
            when(libroRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(libroRepository, never()).delete(any(Libro.class));
        }

        @Test
        void libroExistente_seElimina() {
            Libro existente = libroExistente(1L, "978-1", autor(1L, null), Set.of());
            when(libroRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            verify(libroRepository).delete(existente);
        }
    }
}