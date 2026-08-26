# Tests unitarios — biblioteca-backend

Este documento registra los tests unitarios escritos hasta ahora para la capa `service/` del backend, con el criterio usado y los casos de uso cubiertos por cada uno. Sirve como mapa de cobertura y como referencia rápida de qué se prueba y por qué, sin tener que abrir cada archivo de test.

## Enfoque general

- **Framework**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), sin `@SpringBootTest` — los repositorios y services colaboradores se mockean, no se levanta contexto de Spring.
- **Alcance**: capa `service/` primero, por ser donde vive la lógica de negocio real (validaciones, reglas, orquestación). Controllers y `GlobalExceptionHandler` quedan para una siguiente etapa.
- **Estructura**: cada test class usa `@Nested` para agrupar por método del service (`Crear`, `Actualizar`, `Eliminar`, etc.).
- **Orden de validaciones**: cuando un método valida más de una cosa (ej. existencia de una entidad relacionada + reglas de negocio), se testea explícitamente cuál validación gana cuando ambas fallarían al mismo tiempo, para dejar ese comportamiento documentado y detectar si cambia sin querer en el futuro.

## Estado: **último archivo (`RecomendacionesServiceTest`) tiene errores de compilación sin corregir** — pendiente para la próxima sesión.

---

## `LibroImportacionesServiceTest`

Cubre el flujo de importación externa (`país → autor → géneros → libro`), el más complejo del backend.

- **Resolución de autor**: `autorId` existente (usado directo, sin llamar a `AutorService`) vs. `datos` de autor nuevo; validación de que debe venir uno u otro, nunca ambos ni ninguno (`DatosInvalidosException`).
- **Resolución de país del autor** (dentro de autor nuevo): `paisId` directo sin consultar repositorio; país nuevo por nombre; nombre en blanco → resuelve al placeholder `PAIS_PENDIENTE`; sin país → mismo placeholder; en cada caso de país nuevo/placeholder, el buscar-o-crear (existe → se reutiliza sin duplicar; no existe → se crea).
- **Resolución de géneros**: misma regla XOR que autor (id o datos, no ambos/ninguno); buscar-o-crear por nombre; mezcla de géneros existentes y nuevos en la misma lista preservando el orden; lista de géneros vacía o nula.
- **Armado del libro**: estado por defecto `POR_LEER` cuando no viene especificado; passthrough de datos de lectura personal (año, fecha inicio/término); datos básicos del libro (título, ISBN, portada, año de publicación) propagados tal cual; el resultado de `LibroService.crear()` se devuelve sin modificar.

## `AutorServiceTest`

- **`listarTodos`**: lista vacía y con datos, conversión a DTO.
- **`buscarPorId`**: existente vs. `RecursoNoEncontradoException`.
- **`crear`**: país inexistente (sin guardar); fecha de nacimiento exacta + año aproximado al mismo tiempo (`DatosInvalidosException`); mismo caso para fecha de defunción; **orden de validación** cuando ambas fallan a la vez (país inexistente + fechas inválidas → gana la excepción de país, porque el código valida el país antes que las fechas); caso feliz con verificación completa del mapeo DTO → entidad.
- **`actualizar`**: autor inexistente (ni siquiera consulta país); país inexistente; fechas inválidas; caso feliz — con nota de que `mapearDTOaEntidad` muta la misma instancia obtenida por `findById` en vez de crear una nueva.
- **`eliminar`**: inexistente vs. existente.

## `EstadisticaServiceTest`

- **Por estado** y **por año de lectura**: mapeo simple fila → `ConteoDTO`.
- **Por género**, con y sin filtro de año: verifica el orden descendente por cantidad y que el año se propaga tal cual al repositorio.
- **Ritmo de lectura**: caso normal (cálculo de promedio) y caso borde de lista vacía (evita división por cero, `promedio` queda `null`).
- **Por autor** y **por país**, cada uno con dos variantes:
  - Con año: usa solo la query filtrada por año; el campo `cantidadTotal` queda `null` a propósito (no se combina con totales en ese caso).
  - Sin año: combina el conteo total con el conteo de leídos, incluyendo el caso de una entidad que aparece en totales pero no en leídos (debe defaultear a `0`, no lanzar excepción).

**Nota técnica**: se encontró y corrigió un bug de ambigüedad de varargs de Java al mockear `List.of(new Object[]{...})` con **una sola fila** — el compilador no distingue entre "un elemento `Object[]`" y "el array de varargs en sí", e infiere `List<Object>` en vez de `List<Object[]>`. Se resuelve con el type witness `List.<Object[]>of(...)`. Con dos o más filas no aplica.

## `GeneroServiceTest`

- **`listarTodos`**: vacío y con datos.
- **`crear`**: nombre duplicado (sin guardar) vs. caso feliz.
- **`actualizar`**: género inexistente (ni siquiera consulta duplicado); nombre duplicado perteneciente a **otro** género; caso borde de renombrar un género **a su propio nombre actual** (no debe contar como duplicado — el filtro por id lo excluye correctamente); caso feliz.
- **`eliminar`**: inexistente vs. existente.

## `LibroServiceTest`

El service más grande cubierto hasta ahora.

- **Listados**: `listarTodos` con y sin país del autor (el DTO de autor resumido debe manejar el caso de autor sin país asociado); `listarPorAutor` delegando al repositorio.
- **`buscarConFiltros`**: **limitación documentada** — con mocks puros solo se puede verificar que el service arma una `Specification` y delega al repositorio, y que mapea bien el resultado devuelto. La lógica real de cada predicado (`LibroSpecification.tieneEstado`, `tieneAlgunGenero`, etc.) no es verificable así; necesitaría un `@DataJpaTest` con base H2 en memoria para probarse contra SQL generado de verdad. **Pendiente como tarea futura si se quiere cobertura real de los filtros.**
- **`buscarPorId`**: existente vs. inexistente.
- **`crear`**: ISBN duplicado (antes de consultar el autor); ISBN nulo o en blanco (no dispara la consulta de duplicado); autor inexistente; fecha de inicio posterior a fecha de término (`DatosInvalidosException`), incluyendo el caso borde de fechas iguales (válido, `isAfter` no incluye igualdad); estado inválido (lanza `RecursoNoEncontradoException` — ver nota de diseño abajo); géneros nulos/vacíos vs. con datos; caso feliz.
- **`actualizar`**: libro inexistente (ni siquiera consulta ISBN); ISBN duplicado de **otro** libro vs. del **mismo** libro que se está editando (no debe fallar); caso feliz.
- **`eliminar`**: inexistente vs. existente.

**Nota de diseño** (no corregida, solo documentada): `parsearEstado` lanza `RecursoNoEncontradoException` cuando el string de estado no es válido, en vez de `DatosInvalidosException` — semánticamente parece una inconsistencia ("estado inválido" suena más a datos inválidos que a recurso no encontrado), pero el test prueba el comportamiento real tal como está escrito.

**Notas técnicas de Mockito** encontradas en este archivo:
- `verify(libroRepository, never()).delete(any())` genera un error de sobrecarga ambigua, porque `LibroRepository` también extiende soporte de `Specification` (por `buscarConFiltros`), lo que agrega un `delete(PredicateSpecification<T>)` además del `delete(T entity)` normal. Se resuelve con `any(Libro.class)` en vez de `any()` a secas.
- Mockear un método que recibe `Specification<T>` con `any(Specification.class)` genera un warning de *unchecked conversion* por borrado de tipos. Se resuelve con `ArgumentMatchers.<Specification<Libro>>any()` en vez de `any(Specification.class)`.

## `PaisServiceTest`

El más simple — no tiene método `actualizar` (el CRUD real de país solo expone crear/listar/eliminar).

- **`listarTodos`**: vacío y con datos.
- **`crear`**: nombre duplicado (sin guardar); caso feliz; nota de que la comparación case-insensitive la hace el repositorio (`findByNombreIgnoreCase`), el service no normaliza nada por su cuenta.
- **`eliminar`**: inexistente vs. existente.

## `RecomendacionesServiceTest` — **con errores de compilación pendientes de corregir**

Cubre `obtenerPorAutorPendiente()`, que arma hasta 21 sugerencias recorriendo el ranking de autores por libros leídos y eligiendo un pendiente al azar de cada uno.

Particularidades de este service que afectan el testeo:
- Depende de **otro service** (`EstadisticaService.obtenerConteoPorAutor(null)`), no solo de un repositorio — hay que mockear ambos colaboradores.
- Usa `java.util.Random` instanciado inline dentro del service (no inyectado), así que cuando un autor tiene más de un libro pendiente, el resultado exacto **no es determinístico**. Los tests para ese caso solo verifican que el libro elegido pertenece al conjunto de pendientes del autor (`isIn(...)`), no cuál específicamente. Con un solo pendiente por autor sí es determinístico (`Random.nextInt(1)` siempre da `0`), y esos casos se usan para verificaciones más finas.

Casos cubiertos (pendientes de que compilen):
- Ranking vacío → lista vacía.
- Autor sin pendientes → se salta **sin** consumir cupo (no cuenta como una de las 21 sugerencias).
- Autor con un solo pendiente → elección determinística, verificando id/título/autor.
- Autor con varios pendientes → el elegido pertenece al conjunto, sin asumir cuál.
- Varios autores con un pendiente cada uno → el orden del resultado respeta el orden del ranking.
- Agrupación por `autorId`, no por nombre/título — verificado con dos autores distintos que comparten el mismo título de libro, para asegurar que no se mezclan sus pendientes.
- Cupo máximo de 21: ranking armado con autores sin pendientes al principio (no deben gastar cupo) más 22 autores con un pendiente cada uno, verificando que entran exactamente 21 y que el que se pasa del límite queda excluido.

**Nota abierta**: se asumieron los getters de `SugerenciaLibroDTO` (`getId`/`getTitulo`/`getAutorNombre`/`getPortadaUrl`) a partir del orden posicional del constructor usado en el service, sin haber visto el DTO real — es la causa más probable de los errores de compilación reportados. **Corregir en la próxima sesión.**

---

## Pendiente general

1. **Corregir `RecomendacionesServiceTest`** (errores de compilación, probablemente por los getters asumidos de `SugerenciaLibroDTO`).
2. `GlobalExceptionHandler` — verificar el mapeo de cada excepción de dominio a su status HTTP correspondiente.
3. Controllers (`@WebMvcTest`) — opcional, evaluar si se justifica la inversión de tiempo dado que son mayormente passthrough hacia los services ya cubiertos.
4. `LibroSpecification` — cobertura real de los predicados de filtro vía `@DataJpaTest` con H2, ya que no es verificable con mocks puros (ver nota en `LibroServiceTest`).
5. Evaluar si conviene inyectar `Random` en `RecomendacionesService` (en vez de instanciarlo inline) para poder testear determinísticamente la elección entre varios pendientes — actualmente es una limitación de testeabilidad, no un bug.