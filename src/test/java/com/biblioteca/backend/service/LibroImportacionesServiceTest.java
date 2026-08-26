package com.biblioteca.backend.service;

import com.biblioteca.backend.dto.AutorDTO;
import com.biblioteca.backend.dto.AutorResponseDTO;
import com.biblioteca.backend.dto.LibroDTO;
import com.biblioteca.backend.dto.LibroResponseDTO;
import com.biblioteca.backend.dto.importacion.AutorImportDTO;
import com.biblioteca.backend.dto.importacion.AutorNuevoDTO;
import com.biblioteca.backend.dto.importacion.GeneroImportDTO;
import com.biblioteca.backend.dto.importacion.GeneroNuevoDTO;
import com.biblioteca.backend.dto.importacion.ImportarLibroExternoDTO;
import com.biblioteca.backend.dto.importacion.PaisImportDTO;
import com.biblioteca.backend.dto.importacion.PaisNuevoDTO;
import com.biblioteca.backend.exception.DatosInvalidosException;
import com.biblioteca.backend.model.Genero;
import com.biblioteca.backend.model.Pais;
import com.biblioteca.backend.repository.GeneroRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de LibroImportacionesService. Todos los colaboradores
 * (repositorios y services) van mockeados: como la lógica interna de
 * resolución (resolverAutor, resolverPaisAutor, resolverGeneros,
 * buscarOCrearPais/Genero) es privada, cada test pasa por el único
 * método público, importarLibroExterno(...), y verifica el comportamiento
 * a través de las interacciones con los mocks.
 */
@ExtendWith(MockitoExtension.class)
class LibroImportacionesServiceTest {

    private static final String PAIS_PLACEHOLDER = "PAIS_PENDIENTE";

    @Mock
    private PaisRepository paisRepository;
    @Mock
    private GeneroRepository generoRepository;
    @Mock
    private AutorService autorService;
    @Mock
    private LibroService libroService;

    private LibroImportacionesService service;

    @BeforeEach
    void setUp() {
        service = new LibroImportacionesService(paisRepository, generoRepository, autorService, libroService);
    }


    // --- Helpers de construcción ---

    private ImportarLibroExternoDTO dtoBase(AutorImportDTO autor, List<GeneroImportDTO> generos) {
        ImportarLibroExternoDTO dto = new ImportarLibroExternoDTO();
        dto.setTitulo("Memorabilia");
        dto.setIsbn("978-84-376-0494-7");
        dto.setPortadaUrl("http://ejemplo.com/portada.jpg");
        dto.setAnioPublicacion(1998);
        dto.setAutor(autor);
        dto.setGeneros(generos);
        return dto;
    }

    private void mockLibroServiceOk() {
        LibroResponseDTO respuesta = new LibroResponseDTO();
        when(libroService.crear(any(LibroDTO.class))).thenReturn(respuesta);
    }

    private AutorImportDTO autorConId(Long id) {
        AutorImportDTO autorImport = new AutorImportDTO();
        autorImport.setAutorId(id);
        return autorImport;
    }

    private AutorImportDTO autorNuevo(PaisImportDTO pais) {
        AutorNuevoDTO datos = new AutorNuevoDTO();
        datos.setNombre("Jenofonte");
        datos.setIdioma("griego antiguo");
        datos.setPais(pais);
        datos.setRetratoUrl("http://ejemplo.com/retrato.jpg");
        datos.setFechaNacimiento(null);
        datos.setAnioNacimientoAprox(-430);
        AutorImportDTO autorImport = new AutorImportDTO();
        autorImport.setDatos(datos);
        return autorImport;
    }

    private void mockAutorServiceOk(Long idGenerado) {
        AutorResponseDTO respuesta = new AutorResponseDTO();
        respuesta.setId(idGenerado);
        when(autorService.crear(any(AutorDTO.class))).thenReturn(respuesta);
    }


    // --- Resolución de autor: banda existente/nuevo/inválida ---

    @Nested
    class ResolucionAutor {

        @Test
        void autorConIdExistente_seUsaDirectoSinLlamarAutorService() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(42L), null);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verifyNoInteractions(autorService);
            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getAutorId()).isEqualTo(42L);
        }

        @Test
        void autorConIdYDatosAlMismoTiempo_lanzaExcepcion() {
            AutorImportDTO autorImport = autorConId(42L);
            autorImport.setDatos(new AutorNuevoDTO());
            ImportarLibroExternoDTO dto = dtoBase(autorImport, null);

            assertThatThrownBy(() -> service.importarLibroExterno(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verifyNoInteractions(libroService, autorService, paisRepository);
        }

        @Test
        void autorSinIdNiDatos_lanzaExcepcion() {
            ImportarLibroExternoDTO dto = dtoBase(new AutorImportDTO(), null);

            assertThatThrownBy(() -> service.importarLibroExterno(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verifyNoInteractions(libroService, autorService, paisRepository);
        }

        @Test
        void autorNulo_lanzaExcepcion() {
            ImportarLibroExternoDTO dto = dtoBase(null, null);

            assertThatThrownBy(() -> service.importarLibroExterno(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verifyNoInteractions(libroService, autorService);
        }

        @Test
        void autorNuevo_seCreaViaAutorServiceConDatosCorrectos() {
            PaisImportDTO pais = new PaisImportDTO();
            pais.setPaisId(3L);
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(pais), null);
            mockAutorServiceOk(7L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<AutorDTO> captor = ArgumentCaptor.forClass(AutorDTO.class);
            verify(autorService).crear(captor.capture());
            AutorDTO enviado = captor.getValue();
            assertThat(enviado.getNombre()).isEqualTo("Jenofonte");
            assertThat(enviado.getIdioma()).isEqualTo("griego antiguo");
            assertThat(enviado.getPaisId()).isEqualTo(3L);
            assertThat(enviado.getAnioNacimientoAprox()).isEqualTo(-430);

            ArgumentCaptor<LibroDTO> libroCaptor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(libroCaptor.capture());
            assertThat(libroCaptor.getValue().getAutorId()).isEqualTo(7L);
        }
    }


    // --- Resolución de país del autor nuevo ---

    @Nested
    class ResolucionPaisAutor {

        @Test
        void paisConPaisIdDirecto_seUsaSinConsultarRepositorio() {
            PaisImportDTO pais = new PaisImportDTO();
            pais.setPaisId(9L);
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(pais), null);
            mockAutorServiceOk(1L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verifyNoInteractions(paisRepository);
            ArgumentCaptor<AutorDTO> captor = ArgumentCaptor.forClass(AutorDTO.class);
            verify(autorService).crear(captor.capture());
            assertThat(captor.getValue().getPaisId()).isEqualTo(9L);
        }

        @Test
        void paisNulo_resuelveAPaisPendiente_yLoCreaSiNoExiste() {
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(null), null);
            when(paisRepository.findByNombreIgnoreCase(PAIS_PLACEHOLDER)).thenReturn(Optional.empty());
            Pais paisGuardado = new Pais();
            paisGuardado.setId(99L);
            when(paisRepository.save(any(Pais.class))).thenReturn(paisGuardado);
            mockAutorServiceOk(1L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<Pais> paisCaptor = ArgumentCaptor.forClass(Pais.class);
            verify(paisRepository).save(paisCaptor.capture());
            assertThat(paisCaptor.getValue().getNombre()).isEqualTo(PAIS_PLACEHOLDER);

            ArgumentCaptor<AutorDTO> autorCaptor = ArgumentCaptor.forClass(AutorDTO.class);
            verify(autorService).crear(autorCaptor.capture());
            assertThat(autorCaptor.getValue().getPaisId()).isEqualTo(99L);
        }

        @Test
        void paisNulo_resuelveAPaisPendiente_yLoReutilizaSiYaExiste() {
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(null), null);
            Pais existente = new Pais();
            existente.setId(5L);
            when(paisRepository.findByNombreIgnoreCase(PAIS_PLACEHOLDER)).thenReturn(Optional.of(existente));
            mockAutorServiceOk(1L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verify(paisRepository, never()).save(any());
            ArgumentCaptor<AutorDTO> autorCaptor = ArgumentCaptor.forClass(AutorDTO.class);
            verify(autorService).crear(autorCaptor.capture());
            assertThat(autorCaptor.getValue().getPaisId()).isEqualTo(5L);
        }

        @Test
        void paisConNombreEnBlanco_resuelveAPaisPendiente() {
            PaisImportDTO pais = new PaisImportDTO();
            PaisNuevoDTO datos = new PaisNuevoDTO();
            datos.setNombre("   ");
            pais.setDatos(datos);
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(pais), null);
            when(paisRepository.findByNombreIgnoreCase(PAIS_PLACEHOLDER)).thenReturn(Optional.empty());
            Pais paisGuardado = new Pais();
            paisGuardado.setId(99L);
            when(paisRepository.save(any(Pais.class))).thenReturn(paisGuardado);
            mockAutorServiceOk(1L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verify(paisRepository).findByNombreIgnoreCase(PAIS_PLACEHOLDER);
        }

        @Test
        void paisConNombreNuevo_buscaOCreaPorEseNombre() {
            PaisImportDTO pais = new PaisImportDTO();
            PaisNuevoDTO datos = new PaisNuevoDTO();
            datos.setNombre("Grecia");
            pais.setDatos(datos);
            ImportarLibroExternoDTO dto = dtoBase(autorNuevo(pais), null);
            when(paisRepository.findByNombreIgnoreCase("Grecia")).thenReturn(Optional.empty());
            Pais paisGuardado = new Pais();
            paisGuardado.setId(11L);
            when(paisRepository.save(any(Pais.class))).thenReturn(paisGuardado);
            mockAutorServiceOk(1L);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verify(paisRepository).findByNombreIgnoreCase("Grecia");
            verify(paisRepository, never()).findByNombreIgnoreCase(PAIS_PLACEHOLDER);
        }
    }


    // --- Resolución de géneros ---

    @Nested
    class ResolucionGeneros {

        @Test
        void sinGeneros_devuelveListaVaciaSinTocarRepositorio() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verifyNoInteractions(generoRepository);
            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getGeneroIds()).isEmpty();
        }

        @Test
        void generoConIdYDatosAlMismoTiempo_lanzaExcepcion() {
            GeneroImportDTO genero = new GeneroImportDTO();
            genero.setGeneroId(1L);
            genero.setDatos(new GeneroNuevoDTO());
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), List.of(genero));

            assertThatThrownBy(() -> service.importarLibroExterno(dto))
                    .isInstanceOf(DatosInvalidosException.class);

            verifyNoInteractions(libroService);
        }

        @Test
        void generoSinIdNiDatos_lanzaExcepcion() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), List.of(new GeneroImportDTO()));

            assertThatThrownBy(() -> service.importarLibroExterno(dto))
                    .isInstanceOf(DatosInvalidosException.class);
        }

        @Test
        void generoExistentePorNombre_seReutilizaSinCrear() {
            GeneroNuevoDTO datos = new GeneroNuevoDTO();
            datos.setNombre("Tragedia");
            GeneroImportDTO generoImport = new GeneroImportDTO();
            generoImport.setDatos(datos);
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), List.of(generoImport));

            Genero existente = new Genero();
            existente.setId(4L);
            when(generoRepository.findByNombreIgnoreCase("Tragedia")).thenReturn(Optional.of(existente));
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            verify(generoRepository, never()).save(any());
            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getGeneroIds()).containsExactly(4L);
        }

        @Test
        void generoNuevo_seCreaConNombreEIcono() {
            GeneroNuevoDTO datos = new GeneroNuevoDTO();
            datos.setNombre("Filosofía");
            datos.setIconoSlug("filosofia.png");
            GeneroImportDTO generoImport = new GeneroImportDTO();
            generoImport.setDatos(datos);
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), List.of(generoImport));

            when(generoRepository.findByNombreIgnoreCase("Filosofía")).thenReturn(Optional.empty());
            Genero guardado = new Genero();
            guardado.setId(20L);
            when(generoRepository.save(any(Genero.class))).thenReturn(guardado);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<Genero> generoCaptor = ArgumentCaptor.forClass(Genero.class);
            verify(generoRepository).save(generoCaptor.capture());
            assertThat(generoCaptor.getValue().getNombre()).isEqualTo("Filosofía");
            assertThat(generoCaptor.getValue().getIconoSlug()).isEqualTo("filosofia.png");

            ArgumentCaptor<LibroDTO> libroCaptor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(libroCaptor.capture());
            assertThat(libroCaptor.getValue().getGeneroIds()).containsExactly(20L);
        }

        @Test
        void mezclaDeGenerosExistentesYNuevos_preservaOrden() {
            GeneroImportDTO existenteImport = new GeneroImportDTO();
            existenteImport.setGeneroId(1L);

            GeneroNuevoDTO datosNuevo = new GeneroNuevoDTO();
            datosNuevo.setNombre("Épica");
            GeneroImportDTO nuevoImport = new GeneroImportDTO();
            nuevoImport.setDatos(datosNuevo);

            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), List.of(existenteImport, nuevoImport));

            when(generoRepository.findByNombreIgnoreCase("Épica")).thenReturn(Optional.empty());
            Genero guardado = new Genero();
            guardado.setId(30L);
            when(generoRepository.save(any(Genero.class))).thenReturn(guardado);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getGeneroIds()).containsExactly(1L, 30L);
        }
    }

    
    // --- Armado del libro ---

    @Nested
    class ArmadoLibro {

        @Test
        void estadoNulo_defaultaAPorLeer() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            dto.setEstado(null);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo("POR_LEER");
        }

        @Test
        void estadoProvisto_seRespetaTalCual() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            dto.setEstado("LEIDO");
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo("LEIDO");
        }

        @Test
        void datosDeLecturaPersonal_sePropaganTalCual() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            dto.setAnioLectura(2024);
            dto.setFechaInicio(LocalDate.of(2024, 1, 10));
            dto.setFechaTermino(LocalDate.of(2024, 1, 20));
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            LibroDTO enviado = captor.getValue();
            assertThat(enviado.getAnioLectura()).isEqualTo(2024);
            assertThat(enviado.getFechaInicio()).isEqualTo(LocalDate.of(2024, 1, 10));
            assertThat(enviado.getFechaTermino()).isEqualTo(LocalDate.of(2024, 1, 20));
        }

        @Test
        void datosBasicosDelLibro_sePropaganTalCual() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            mockLibroServiceOk();

            service.importarLibroExterno(dto);

            ArgumentCaptor<LibroDTO> captor = ArgumentCaptor.forClass(LibroDTO.class);
            verify(libroService).crear(captor.capture());
            LibroDTO enviado = captor.getValue();
            assertThat(enviado.getTitulo()).isEqualTo("Memorabilia");
            assertThat(enviado.getIsbn()).isEqualTo("978-84-376-0494-7");
            assertThat(enviado.getPortadaUrl()).isEqualTo("http://ejemplo.com/portada.jpg");
            assertThat(enviado.getAnioPublicacion()).isEqualTo(1998);
        }

        @Test
        void resultadoDeLibroService_seDevuelveTalCual() {
            ImportarLibroExternoDTO dto = dtoBase(autorConId(1L), null);
            LibroResponseDTO respuestaEsperada = new LibroResponseDTO();
            when(libroService.crear(any(LibroDTO.class))).thenReturn(respuestaEsperada);

            LibroResponseDTO resultado = service.importarLibroExterno(dto);

            assertThat(resultado).isSameAs(respuestaEsperada);
        }
    }
}