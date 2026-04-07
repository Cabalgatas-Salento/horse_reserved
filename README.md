# Horse Reserved — Backend API

API REST para el sistema de reserva de cabalgatas. Gestiona autenticación de usuarios, rutas, salidas, reservaciones y envío de correos transaccionales.

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.0 |
| Build | Gradle 8+ |
| Base de datos | PostgreSQL 17 |
| Migraciones | Flyway |
| Persistencia | Spring Data JPA / Hibernate |
| Autenticación | JWT (JJWT 0.12.3) + OAuth2 Google |
| Seguridad | Spring Security, BCrypt (strength 12) |
| Email | Spring Mail — Gmail SMTP |
| Boilerplate | Lombok, Jakarta Validation |

---

## Estructura del proyecto

```
src/main/java/horse_reserved/
├── ReservaDeCabalgatasApplication.java   # Punto de entrada (@EnableAsync)
├── config/
│   └── SecurityConfig.java               # Filtros JWT, OAuth2, CORS, rutas públicas
├── controller/
│   └── AuthController.java               # Endpoints /api/auth/**
├── service/
│   ├── AuthService.java                  # Registro, login, perfil, cambio de contraseña
│   ├── JwtService.java                   # Generación y validación de tokens JWT
│   ├── PasswordResetService.java         # Flujo de recuperación de contraseña
│   ├── EmailService.java                 # Envío asíncrono de correos (SMTP)
│   ├── CustomOAuth2UserService.java      # Procesamiento de usuario OAuth2 Google
│   └── CustomUserDetailsService.java     # Carga de usuario por email
├── security/
│   ├── JwtAuthenticationFilter.java      # Extrae y valida JWT en cada request
│   ├── OAuth2AuthenticationSuccessHandler.java
│   └── OAuth2AuthenticationFailureHandler.java
├── model/
│   ├── Usuario.java                      # Entidad usuario (implements UserDetails)
│   ├── Rol.java                          # Enum: CLIENTE, OPERADOR, ADMINISTRADOR
│   ├── TipoDocumento.java                # Enum: CEDULA, PASAPORTE, TARJETA_IDENTIDAD
│   └── PasswordResetToken.java           # Tokens de recuperación de contraseña
├── dto/
│   ├── request/                          # LoginRequest, RegisterRequest, etc.
│   └── response/                         # AuthResponse, UserProfileResponse, ErrorResponse
├── repository/
│   ├── UsuarioRepository.java
│   └── PasswordResetTokenRepository.java
└── exception/
    └── GlobalExceptionHandler.java       # @RestControllerAdvice — manejo centralizado de errores
```

---

## Esquema de base de datos

Las migraciones Flyway se aplican automáticamente al arrancar la aplicación.

### `usuarios`
| Columna | Tipo | Notas |
|---------|------|-------|
| id | BIGSERIAL PK | |
| primer_nombre | VARCHAR(100) | NOT NULL |
| primer_apellido | VARCHAR(100) | NOT NULL |
| tipo_documento | VARCHAR(50) | CHECK: CEDULA, PASAPORTE, TARJETA_IDENTIDAD |
| documento | VARCHAR(50) | NOT NULL |
| email | VARCHAR(200) | NOT NULL UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL (vacío para usuarios OAuth2) |
| telefono | VARCHAR(20) | |
| role | VARCHAR(50) | DEFAULT 'cliente' · CHECK: cliente, operador, administrador |
| is_active | BOOLEAN | DEFAULT TRUE |

### `password_reset_tokens`
| Columna | Tipo | Notas |
|---------|------|-------|
| id | BIGSERIAL PK | |
| token | VARCHAR(255) | NOT NULL UNIQUE · UUID generado por backend |
| usuario_id | BIGINT FK | → usuarios.id CASCADE |
| expires_at | TIMESTAMP | created_at + 30 minutos |
| used | BOOLEAN | DEFAULT FALSE · uso único |
| created_at | TIMESTAMP | DEFAULT NOW() |

### Otras tablas (reservaciones)
`rutas` · `salidas` · `caballos` · `guias` · `salida_caballos` · `salida_guias` · `reservaciones` · `participantes`

---

## API de autenticación

Base URL: `http://localhost:8080/api/auth`

---

### `POST /api/auth/register`
Registra una nueva cuenta de cliente.

**Request body:**
```json
{
  "primerNombre": "Camila",
  "primerApellido": "Castro",
  "tipoDocumento": "CEDULA",
  "documento": "1234567890",
  "email": "camila@email.com",
  "password": "MiPassword123",
  "telefono": "3001234567"
}
```

**Response `201 Created`:**
```json
{
  "token": "<JWT>",
  "type": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "camila@email.com",
  "primerNombre": "Camila",
  "primerApellido": "Castro",
  "role": "CLIENTE"
}
```

---

### `POST /api/auth/login`
Autentica con email y contraseña.

**Request body:**
```json
{
  "email": "camila@email.com",
  "password": "MiPassword123"
}
```

**Response `200 OK`:** igual que `/register`.

| Error | Status |
|-------|--------|
| Credenciales incorrectas | `401` |
| Usuario inactivo | `403` |

---

### `GET /api/auth/me`
Devuelve el perfil del usuario autenticado.

**Headers:** `Authorization: Bearer <JWT>`

**Response `200 OK`:**
```json
{
  "userId": 1,
  "email": "camila@email.com",
  "primerNombre": "Camila",
  "primerApellido": "Castro",
  "tipoDocumento": "CEDULA",
  "documento": "1234567890",
  "telefono": "3001234567",
  "role": "CLIENTE",
  "isActive": true
}
```

---

### `PUT /api/auth/change-password`
Cambia la contraseña del usuario autenticado.

**Headers:** `Authorization: Bearer <JWT>`

**Request body:**
```json
{
  "passwordActual": "MiPassword123",
  "passwordNueva": "NuevaPassword456",
  "confirmarPassword": "NuevaPassword456"
}
```

**Response `200 OK`:** `"Contraseña actualizada correctamente"`

---

### `POST /api/auth/forgot-password`
Inicia el flujo de recuperación de contraseña.

Siempre responde `200 OK` (previene enumeración de emails).

**Request body:**
```json
{
  "email": "camila@email.com"
}
```

**Comportamiento interno:**
1. Busca el usuario por email
2. Invalida tokens anteriores no usados
3. Genera token UUID con expiración de 30 minutos
4. Envía email HTML con enlace: `/auth/reset-password?token=<UUID>`

---

### `POST /api/auth/reset-password`
Valida el token y establece la nueva contraseña.

**Request body:**
```json
{
  "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "nuevaPassword": "NuevaPassword456"
}
```

**Response `200 OK`:** `"Contraseña restablecida correctamente. Ya puedes iniciar sesión"`

| Error | Status |
|-------|--------|
| Token inválido, expirado o ya usado | `400` |

---

### OAuth2 Google

| Acción | URL |
|--------|-----|
| Iniciar login con Google | `GET /oauth2/authorization/google` |
| Redirect tras éxito | `http://localhost:4200/auth/oauth2-redirect?token=<JWT>&email=<email>&role=<role>` |

---

## Autorización por rol

| Ruta | Rol requerido |
|------|--------------|
| `/api/auth/**` | Público |
| `/api/rutas/public/**` | Público |
| `/api/reservas/**` | CLIENTE, OPERADOR, ADMINISTRADOR |
| `/api/salidas/**` | OPERADOR, ADMINISTRADOR |
| `/api/admin/**` | ADMINISTRADOR |
| `/api/rutas/**` | ADMINISTRADOR |
| `/api/recursos/**` | ADMINISTRADOR |

---

## Manejo de errores

Todas las respuestas de error siguen el formato:

```json
{
  "timestamp": "2026-02-27T18:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "path": "/api/auth/login"
}
```

| Excepción | Status |
|-----------|--------|
| `EmailAlreadyExistsException` | `409` |
| `InvalidCredentialsException` | `401` |
| `InvalidTokenException` | `400` |
| `UserInactiveException` | `403` |
| Validación (`@Valid`) | `400` (por campo) |
| Error interno | `500` |

---

## Variables de entorno

Crear el archivo `src/main/resources/env.properties` (está en `.gitignore`):

```properties
# Google OAuth2 — console.cloud.google.com
GOOGLE_CLIENT_ID=<tu_client_id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<tu_client_secret>

# Gmail SMTP — myaccount.google.com/apppasswords (sin espacios)
GMAIL_USERNAME=<tu_correo>@gmail.com
GMAIL_APP_PASSWORD=<contraseña_de_aplicacion_16_chars>
```

> La contraseña de aplicación de Gmail se genera con la verificación en 2 pasos activa.
> Ingresar los 16 caracteres **sin espacios**.

---

## Cómo ejecutar

### Requisitos previos
- Java 21+
- PostgreSQL 17+ corriendo
- `env.properties` configurado

### 1. Levantar PostgreSQL con Docker

```bash
cd docker
docker compose up -d
```

Esto crea:
- Base de datos: `cabalgatas_db`
- Usuario: `cabalgatas_user` / `cabalgatas_pass`
- Puerto: `5432`

### 2. Ejecutar la aplicación

```bash
./gradlew bootRun
```

La API queda disponible en `http://localhost:8080`.

Flyway aplica automáticamente todas las migraciones en el primer arranque.

### 3. Compilar JAR

```bash
./gradlew clean build
java -jar build/libs/horse_reserved-0.0.1-SNAPSHOT.jar
```

### 4. Ejecutar tests

```bash
./gradlew test
```

---

## Seguridad

- **Contraseñas:** BCrypt con `strength = 12`
- **JWT:** firma HS512, expiración de 24 horas, sin estado (stateless)
- **OAuth2:** Google — los usuarios OAuth2 tienen `passwordHash` vacío y no pueden usar recuperación de contraseña
- **CSRF:** deshabilitado (API stateless)
- **CORS:** restringido a `http://localhost:4200`
- **Enumeración de emails:** el endpoint `/forgot-password` siempre responde `200`
- **Tokens de recuperación:** UUID de un solo uso, expiran en 30 minutos

## SonarCloud & Cobertura

### Secrets requeridos en GitHub
| Secret | Descripción |
|---|---|
| `SONAR_TOKEN` | Token de autenticación generado en sonarcloud.io |

### Configurar SonarCloud
1. Ir a [sonarcloud.io](https://sonarcloud.io) → importar repositorio de GitHub
2. Anotar `Project Key` y `Organization Key`
3. Reemplazar en `build.gradle`:
    - `REEMPLAZAR_SONAR_PROJECT_KEY`
    - `REEMPLAZAR_SONAR_ORGANIZATION`
4. En GitHub → Settings → Secrets → Actions → New secret → `SONAR_TOKEN`

### Ejecutar análisis local
```bash
./gradlew clean test jacocoTestReport
./gradlew sonar -Dsonar.token=TU_TOKEN_AQUI
```

### Ver reporte de cobertura local
```
build/reports/jacoco/test/html/index.html
```