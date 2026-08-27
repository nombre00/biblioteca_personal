package com.biblioteca.backend.exception;

import com.biblioteca.backend.dto.ErrorResponseDTO;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de GlobalExceptionHandler. Se instancia directo (no
 * necesita mocks propios, es puro mapeo excepción -> ResponseEntity),
 * salvo MethodArgumentNotValidException, que se mockea porque su
 * constructor real exige un MethodParameter que no aporta nada armar a
 * mano para este test — solo se stubea getBindingResult() con un
 * BindingResult real con FieldErrors reales.
 *
 * Nota: los getters de ErrorResponseDTO (getTimestamp/getStatus/getError/
 * getMensaje) se asumen a partir de los nombres de parámetro del
 * constructor — ajustar si el DTO real usa otros nombres.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    class ManejarNoEncontrado {

        @Test
        void devuelve404ConMensajeDeLaExcepcion() {
            RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Libro no encontrado con id: 99");

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarNoEncontrado(ex);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(404);
            assertThat(body.getError()).isEqualTo("Recurso no encontrado");
            assertThat(body.getMensaje()).isEqualTo("Libro no encontrado con id: 99");
            assertThat(body.getTimestamp()).isNotNull();
        }
    }

    @Nested
    class ManejarDuplicado {

        @Test
        void devuelve409ConMensajeDeLaExcepcion() {
            RecursoDuplicadoException ex = new RecursoDuplicadoException("Ya existe un libro con el ISBN: 978-1");

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarDuplicado(ex);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(409);
            assertThat(body.getError()).isEqualTo("Recurso duplicado");
            assertThat(body.getMensaje()).isEqualTo("Ya existe un libro con el ISBN: 978-1");
        }
    }

    @Nested
    class ManejarDatosInvalidos {

        @Test
        void devuelve400ConMensajeDeLaExcepcion() {
            DatosInvalidosException ex = new DatosInvalidosException(
                    "La fecha de inicio de lectura no puede ser posterior a la fecha de término");

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarDatosInvalidos(ex);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getError()).isEqualTo("Datos inválidos");
            assertThat(body.getMensaje())
                    .isEqualTo("La fecha de inicio de lectura no puede ser posterior a la fecha de término");
        }
    }

    @Nested
    class ManejarValidacion {

        @Test
        void unSoloErrorDeCampo_devuelve400ConMensajeDeEseCampo() {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "libroDTO");
            bindingResult.addError(new FieldError("libroDTO", "titulo", "no puede estar vacío"));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarValidacion(ex);

            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getError()).isEqualTo("Error de validación");
            assertThat(body.getMensaje()).isEqualTo("titulo: no puede estar vacío");
        }

        @Test
        void variosErroresDeCampo_seUnenConPuntoYComaEnOrden() {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "libroDTO");
            bindingResult.addError(new FieldError("libroDTO", "titulo", "no puede estar vacío"));
            bindingResult.addError(new FieldError("libroDTO", "isbn", "formato inválido"));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarValidacion(ex);

            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getMensaje())
                    .isEqualTo("titulo: no puede estar vacío; isbn: formato inválido");
        }

        @Test
        void sinErroresDeCampo_devuelveMensajeVacio() {
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "libroDTO");
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ErrorResponseDTO> respuesta = handler.manejarValidacion(ex);

            ErrorResponseDTO body = respuesta.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getMensaje()).isEmpty();
        }
    }
}