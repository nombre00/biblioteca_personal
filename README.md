# biblioteca-backend

Microservicio backend de **Biblioteca**, una aplicación personal de gestión de biblioteca. Expone la API REST principal del dominio (libros, autores, géneros, países, estadísticas y recomendaciones) y persiste el estado en MySQL.

Este servicio es parte de una arquitectura de 4 componentes (frontend, backend, `agentes-ia`, gateway), coordinados desde un meta-repositorio que apunta a los 4 repos individuales. El **gateway** enruta el tráfico externo hacia este servicio; el **backend** no se comunica de forma saliente con `agentes-ia` — es un servicio pasivo en ese flujo: solo recibe datos ya resueltos vía `/api/libros/importar-externo`.

## Stack técnico

- **Java 21**
- **Spring Boot 4.1.0** (`spring-boot-starter-parent`)
- **Spring Data JPA** / Hibernate — persistencia y generación de esquema (`ddl-auto=update`)
- **Spring Security** — stateless, autenticación vía JWT interno
- **Spring Web MVC** (`spring-boot-starter-webmvc`)
- **MySQL** (`mysql-connector-j`)
- **jjwt 0.12.6** (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) — validación de JWT
- **Lombok** — reducción de boilerplate en entidades y DTOs
- **Bean Validation** (`spring-boot-starter-validation`)
- **Maven** (con wrapper `mvnw`)

## Arquitectura interna

Organización por capas, dentro de `com.biblioteca.backend`:

```
controller/      Endpoints REST
service/         Lógica de negocio
repository/      Interfaces Spring Data JPA + Specifications
model/           Entidades JPA
dto/             DTOs de entrada/salida
dto/importacion/ DTOs específicos del flujo de importación externa
exception/       Excepciones de dominio + manejador global
security/        Filtro JWT, validación de token, configuración de seguridad
```

Patrón por dominio: cada entidad relevante (`Libro`, `Autor`, `Genero`, `Pais`) tiene su controller, service y repository. `LibroImportacionesController`/`Service` es un caso aparte: no es CRUD, sino un orquestador que reutiliza los services existentes para resolver un flujo de alta compuesta.

## Autenticación y seguridad

La autenticación de usuario final (Firebase) ocurre en el Gateway. Este backend valida un **JWT interno** propio, no el token de Firebase:

- Header esperado: `X-Internal-Token`
- Firma: HMAC-SHA (clave simétrica en Base64, `jwt.internal.secret`)
- `JwtAuthenticationFilter` extrae el `subject` del token (uid de Firebase) y lo coloca en el `SecurityContext` como principal autenticado, sin roles/authorities.
- Si el token es inválido o falta, el filtro simplemente no autentica — no lanza error directamente; es Spring Security quien rechaza más adelante según la regla de la ruta.

Reglas de acceso (`SecurityConfig`):
- `/api/**` → requiere autenticación (token válido)
- Cualquier otra ruta → permitida sin autenticación
- CSRF deshabilitado, sesiones stateless (no hay `HttpSession`)

## Modelo de datos

| Entidad | Campos clave | Relaciones |
|---|---|---|
| `Libro` | `titulo`, `isbn`, `portadaUrl`, `estado` (enum), `anioPublicacion`, `anioLectura`, `fechaInicio`, `fechaTermino` | `ManyToOne → Autor` (obligatorio), `ManyToMany → Genero` (tabla `libro_genero`) |
| `Autor` | `nombre`, `idioma`, `retratoUrl`, `fechaNacimiento`/`anioNacimientoAprox`, `fechaDefuncion`/`anioDefuncionAprox` | `ManyToOne → Pais` (opcional) |
| `Genero` | `nombre` (único), `iconoSlug` | — |
| `Pais` | `nombre` (único) | — |

`EstadoLibro` es un enum: `POR_LEER`, `LEYENDO`, `LEIDO`.

Las fechas de nacimiento/defunción del autor tienen versión exacta (`LocalDate`) y aproximada (`Integer` año), pensado para datos que vienen de fuentes externas con precisión variable (ej. Wikidata vía `agentes-ia`).

## Endpoints

Todos bajo `/api`. Todos requieren `X-Internal-Token` válido (regla `/api/**` en `SecurityConfig`).

### Libros — `/api/libros`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Lista todos los libros |
| GET | `/buscar` | Búsqueda con filtros combinables (query params de `LibroFiltroDTO`: `estado`, `generoIds`, `paisAutorId`, `idiomaAutor`, `texto`) vía `Specification` |
| GET | `/autor/{autorId}` | Libros de un autor |
| GET | `/{id}` | Detalle por id |
| POST | `/` | Crea (valida ISBN duplicado y coherencia de fechas inicio/término) |
| PUT | `/{id}` | Actualiza |
| DELETE | `/{id}` | Elimina |
| POST | `/importar-externo` | Alta compuesta desde datos externos ya resueltos (ver sección siguiente) |

### Autores — `/api/autores`
CRUD estándar: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.

### Géneros — `/api/generos`
`GET /`, `POST /`, `PUT /{id}`, `DELETE /{id}` (sin `GET /{id}` individual).

### Países — `/api/paises`
`GET /`, `POST /`, `DELETE /{id}` (sin actualización).

### Estadísticas — `/api/estadisticas`
| Ruta | Descripción |
|---|---|
| `GET /por-estado` | Conteo de libros agrupado por `EstadoLibro` |
| `GET /por-genero?anio=` | Conteo por género; si se pasa `anio`, filtra por año de lectura |
| `GET /por-anio-lectura` | Conteo agrupado por año de lectura |
| `GET /ritmo-lectura` | Total de libros leídos, cantidad de años activos y promedio (derivado del conteo por año, no es query propia) |
| `GET /por-autor?anio=` | Conteo por autor; sin `anio`, retorna total + leídos por autor (para dashboards tipo "leídos vs. restante") |
| `GET /por-pais?anio=` | Igual que por-autor, pero agrupado por país del autor |

### Recomendaciones — `/api/recomendaciones`
| Ruta | Descripción |
|---|---|
| `GET /por-autor-pendiente` | Hasta 21 sugerencias: recorre el ranking de autores por libros leídos (desc) y elige al azar un libro `POR_LEER` de cada uno, saltando autores sin pendientes |

## Flujo de importación externa

`POST /api/libros/importar-externo` es el punto de entrada de un flujo de alta compuesta pensado para libros resueltos externamente (Google Books / Wikidata, vía `agentes-ia`). El backend **no llama a nadie**: recibe un `ImportarLibroExternoDTO` ya armado y orquesta la creación en cascada `país → autor → géneros → libro` dentro de una única transacción (`LibroImportacionesService`).

Reglas del payload:
- `autor`: debe traer **o** `autorId` (autor existente) **o** `datos` (autor nuevo) — nunca ambos, nunca ninguno.
- Cada elemento de `generos`: misma regla, `generoId` **o** `datos`.
- `autor.datos.pais`: opcional. Si no viene, se resuelve contra un país placeholder `PAIS_PENDIENTE` (se busca-o-crea, no se duplica entre importaciones).
- `estado`: si viene `null`, se asume `POR_LEER`.

Semántica de creación:
- **País y género**: *buscar o crear* (idempotente por nombre, case-insensitive) — no lanzan error si ya existen, a diferencia de sus endpoints CRUD normales.
- **Autor**: siempre se crea vía `AutorService.crear()` cuando no viene `autorId`, sin chequeo de duplicado (autor nuevo nunca lo tuvo).
- **Libro**: se reutiliza `LibroService.crear()` completo, con sus validaciones normales (ISBN duplicado, coherencia de fechas).

## Manejo de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza las respuestas de error en `ErrorResponseDTO` (timestamp, status, error, mensaje):

| Excepción | HTTP |
|---|---|
| `RecursoNoEncontradoException` | 404 |
| `RecursoDuplicadoException` | 409 |
| `DatosInvalidosException` | 400 |
| `MethodArgumentNotValidException` (Bean Validation) | 400, con mensajes por campo |

## Configuración

Variables de entorno (`.env`, cargado con `source .env` antes de levantar el servicio):

```bash
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_INTERNAL_SECRET=      # clave HMAC en Base64, compartida con quien emite el token interno
BACKEND_URL=http://localhost:8082
```

`application.properties` relevante:
- Puerto: `8082`
- `spring.jpa.hibernate.ddl-auto=update` — el esquema se actualiza automáticamente al arrancar
- `spring.jpa.show-sql=true` con SQL formateado — útil en desarrollo, revisar si conviene desactivar en un entorno más productivo

## Cómo levantar el servicio localmente

Requisitos: JDK 21, MySQL corriendo (vía Laragon u otro), Maven (o usar el wrapper incluido).

```bash
# 1. Configurar variables de entorno
cp .env.example .env
# completar DB_URL, DB_USERNAME, DB_PASSWORD, JWT_INTERNAL_SECRET

# 2. Cargar variables y levantar
source .env
mvn spring-boot:run
# o, con el wrapper:
./mvnw spring-boot:run
```

El servicio queda disponible en `http://localhost:8082`. Para pruebas de endpoints bajo `/api/**`, se necesita un JWT interno válido en el header `X-Internal-Token`.

## Decisiones de diseño

- **JWT interno separado de Firebase**: el Gateway valida la identidad del usuario (Firebase) y el backend valida un token propio firmado con una clave compartida — desacopla la validación de identidad de negocio de la capa de autenticación de usuario.

- **Buscar-o-crear solo donde tiene sentido**: los endpoints CRUD normales de país/género rechazan duplicados (comportamiento correcto para edición manual desde el frontend), pero el flujo de importación necesita idempotencia — de ahí que `LibroImportacionesService` no reutilice esos métodos de `crear()` para país y género, y sí lo haga para autor y libro, donde el chequeo de duplicado es deseable o inexistente.
- **`PAIS_PENDIENTE` como placeholder**: en vez de dejar el país nulo cuando Wikidata no lo resuelve, se usa un país real reutilizable en la base — evita registros huérfanos y facilita encontrar después los autores con país sin resolver.
- **Backend pasivo frente a `agentes-ia`**: el backend no conoce la URL ni las credenciales de `agentes-ia` ni de Google Books — solo expone un endpoint receptor. Esto mantiene la lógica de resolución externa (búsqueda, desambiguación, LLM) completamente fuera del backend, que se limita a persistir.