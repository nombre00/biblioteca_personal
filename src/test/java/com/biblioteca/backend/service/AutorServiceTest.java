package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.AutorDTO;
import com.biblioteca.backend.dto.AutorResponseDTO;
import com.biblioteca.backend.exception.DatosInvalidosException;
import com.biblioteca.backend.exception.RecursoNoEncontradoException;
import com.biblioteca.backend.model.Autor;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.AutorRepository;
import com.biblioteca.backend.repository.PaisRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    @Mock
    private AutorRepository autorRepository;
    @Mock
    private PaisRepository paisRepository;

    private AutorService service;

    @BeforeEach
    void setUp() {
        service = new AutorService(autorRepository, paisRepository);
    }

    // --- Helpers ---

    private Pais paisConId(Long id, String nombre) {
        Pais pais = new Pais();
        pais.setId(id);
        pais.setNombre(nombre);
        return pais;
    }

    private Autor autorExistente(Long id, Long paisId) {
        Autor autor = new Autor();
        autor.setId(id);
        autor.setNombre("Jenofonte");
        autor.setIdioma("griego antiguo");
        autor.setPais(paisConId(paisId, "Grecia"));
        return autor;
    }

    private AutorDTO dtoValido(Long paisId) {
        AutorDTO dto = new AutorDTO();
        dto.setNombre("Jenofonte");
        dto.setIdioma("griego antiguo");
        dto.setPaisId(paisId);
        dto.setRetratoUrl("http://ejemplo.com/retrato.jpg");
        return dto;
    }

    // --- listarTodos ---

    @Nested
    class ListarTodos {

        @Test
        void devuelveListaVaciaSiNoHayAutores() {
            when(autorRepository.findAll()).thenReturn(List.of());

            List<AutorResponseDTO> resultado = service.listarTodos();

            assertThat(resultado).isEmpty();
        }

        @Test
        void devuelveTodosLosAutoresConvertidosADto() {
            Autor autor1 = autorExistente(1L, 10L);
            Autor autor2 = autorExistente(2L, 10L);
            when(autorRepository.findAll()).thenReturn(List.of(autor1, autor2));

            List<AutorResponseDTO> resultado = service.listarTodos();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getId()).isEqualTo(1L);
            assertThat(resultado.get(1).getId()).isEqualTo(2L);
        }
    }

    // --- buscarPorId ---

    @Nested
    class BuscarPorId {

        @Test
        void devuelveDtoSiExiste() {
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente(1L, 10L)));

            AutorResponseDTO resultado = service.buscarPorId(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getPais().getId()).isEqualTo(10L);
        }

        @Test
        void lanzaExcepcionSiNoExiste() {
            when(autorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // --- crear ---

    @Nested
    class Crear {

        @Test
        void paisInexistente_lanzaExcepcionSinGuardar() {
            AutorDTO dto = dtoValido(10L);
            when(paisRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(autorRepository, never()).save(any());
        }

        @Test
        void fechaNacimientoExactaYAproximadaAlMismoTiempo_lanzaExcepcion() {
            AutorDTO dto = dtoValido(10L);
            dto.setFechaNacimiento(LocalDate.of(-430, 1, 1));
            dto.setAnioNacimientoAprox(-430);
            when(paisRepository.findById(10L)).thenReturn(Optional.of(paisConId(10L, "Grecia")));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verify(autorRepository, never()).save(any());
        }

        @Test
        void fechaDefuncionExactaYAproximadaAlMismoTiempo_lanzaExcepcion() {
            AutorDTO dto = dtoValido(10L);
            dto.setFechaDefuncion(LocalDate.of(-354, 1, 1));
            dto.setAnioDefuncionAprox(-354);
            when(paisRepository.findById(10L)).thenReturn(Optional.of(paisConId(10L, "Grecia")));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verify(autorRepository, never()).save(any());
        }

        @Test
        void paisInexistenteYFechasInvalidas_priorizaExcepcionDePais() {
            // La validación de país ocurre antes que la de fechas en
            // mapearDTOaEntidad, así que si ambas fallan, gana la de país.
            AutorDTO dto = dtoValido(10L);
            dto.setFechaNacimiento(LocalDate.of(-430, 1, 1));
            dto.setAnioNacimientoAprox(-430);
            when(paisRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        void datosValidos_guardaYDevuelveDtoCorrecto() {
            AutorDTO dto = dtoValido(10L);
            when(paisRepository.findById(10L)).thenReturn(Optional.of(paisConId(10L, "Grecia")));

            Autor guardado = autorExistente(1L, 10L);
            when(autorRepository.save(any(Autor.class))).thenReturn(guardado);

            AutorResponseDTO resultado = service.crear(dto);

            ArgumentCaptor<Autor> captor = ArgumentCaptor.forClass(Autor.class);
            verify(autorRepository).save(captor.capture());
            Autor enviado = captor.getValue();
            assertThat(enviado.getNombre()).isEqualTo("Jenofonte");
            assertThat(enviado.getIdioma()).isEqualTo("griego antiguo");
            assertThat(enviado.getPais().getId()).isEqualTo(10L);
            assertThat(enviado.getRetratoUrl()).isEqualTo("http://ejemplo.com/retrato.jpg");

            assertThat(resultado.getId()).isEqualTo(1L);
        }
    }

    // --- actualizar ---

    @Nested
    class Actualizar {

        @Test
        void autorInexistente_lanzaExcepcionSinConsultarPais() {
            AutorDTO dto = dtoValido(10L);
            when(autorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizar(99L, dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(paisRepository, never()).findById(any());
            verify(autorRepository, never()).save(any());
        }

        @Test
        void paisInexistente_lanzaExcepcionSinGuardar() {
            AutorDTO dto = dtoValido(20L);
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente(1L, 10L)));
            when(paisRepository.findById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizar(1L, dto))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(autorRepository, never()).save(any());
        }

        @Test
        void fechasInvalidas_lanzaExcepcion() {
            AutorDTO dto = dtoValido(10L);
            dto.setFechaNacimiento(LocalDate.of(-430, 1, 1));
            dto.setAnioNacimientoAprox(-430);
            when(autorRepository.findById(1L)).thenReturn(Optional.of(autorExistente(1L, 10L)));
            when(paisRepository.findById(10L)).thenReturn(Optional.of(paisConId(10L, "Grecia")));

            assertThatThrownBy(() -> service.actualizar(1L, dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verify(autorRepository, never()).save(any());
        }

        @Test
        void datosValidos_actualizaEntidadExistenteYDevuelveDto() {
            Autor existente = autorExistente(1L, 10L);
            AutorDTO dto = dtoValido(20L);
            dto.setNombre("Jenofonte de Atenas");
            when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(paisRepository.findById(20L)).thenReturn(Optional.of(paisConId(20L, "Esparta")));
            when(autorRepository.save(any(Autor.class))).thenReturn(existente);

            AutorResponseDTO resultado = service.actualizar(1L, dto);

            // Se muta la misma instancia obtenida por findById, no una nueva.
            assertThat(existente.getNombre()).isEqualTo("Jenofonte de Atenas");
            assertThat(existente.getPais().getId()).isEqualTo(20L);
            verify(autorRepository).save(existente);
            assertThat(resultado.getId()).isEqualTo(1L);
        }
    }

    // --- eliminar ---

    @Nested
    class Eliminar {

        @Test
        void autorInexistente_lanzaExcepcionSinLlamarDelete() {
            when(autorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(autorRepository, never()).delete(any());
        }

        @Test
        void autorExistente_seElimina() {
            Autor existente = autorExistente(1L, 10L);
            when(autorRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            verify(autorRepository).delete(existente);
        }
    }
}