# API Reference: `usuarios-service`

> **Puerto:** `8081`  
> **URL Base:** `http://<host>:8081/api/v1`  
> **E2E:** Activo en `/usuario-service/*`. Requiere `x-session-id` si se usa cifrado.  

---

## Autenticación

### `POST /auth/login`
Autentica un usuario y devuelve tokens JWT.

**Acceso:** Público (sin autenticación)

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "codigo": 12345,
  "password": "TuPassword123!"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userInfo": {
    "codigo": 12345,
    "nombre": "Juan Pérez",
    "email": "juan@uceva.edu.co",
    "rol": "ADMINISTRADOR",
    "ultimoInicioSesion": "2026-05-26T10:00:00"
  }
}
```

**Errores comunes:**
- `401 Unauthorized`: Credenciales incorrectas
- `404 Not Found`: Usuario no existe

---

### `POST /auth/refresh`
Renueva el token de acceso usando el refresh token.

**Acceso:** Público (requiere `Authorization: Bearer <refresh_token>`)

**Response 200:** Misma estructura que `/auth/login`.

---

## Usuarios

### `GET /usuario-service/usuarios`
Lista todos los usuarios registrados.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Headers:**
```
Authorization: Bearer <jwt_token>
x-session-id: <session_id>   // Solo si usas E2E
```

**Response 200:**
```json
{
  "usuarios": [
    {
      "codigo": 12345,
      "nombre": "Juan",
      "apellido": "Pérez",
      "email": "juan@uceva.edu.co",
      "rol": "ADMINISTRADOR"
    }
  ]
}
```

---

### `GET /usuario-service/usuarios/page/{page}`
Lista usuarios con paginación (tamaño de página: 4).

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Response 200:** Objeto `Page<Usuario>`.

---

### `POST /usuario-service/usuarios`
Crea un nuevo usuario.

**Acceso:** `ADMINISTRADOR` (cualquier rol), `ADMINISTRATIVO` (solo `DOCENTE` o `ESTUDIANTE`)

**Request Body:**
```json
{
  "nombre": "María",
  "apellido": "García",
  "email": "maria@uceva.edu.co",
  "password": "Password123!",
  "rol": "ESTUDIANTE"
}
```

**Response 201:**
```json
{
  "mensaje": "El usuario ha sido creado con éxito!",
  "usuario": {
    "codigo": 12346,
    "nombre": "María",
    "apellido": "García",
    "email": "maria@uceva.edu.co",
    "rol": "ESTUDIANTE"
  }
}
```

**Response 403:** Si un `ADMINISTRATIVO` intenta crear un `ADMINISTRADOR`:
```json
{
  "mensaje": "No tienes permiso para crear usuarios con el rol ADMINISTRADOR"
}
```

> **Nota de seguridad:** El campo `password` se oculta en todas las respuestas JSON mediante `@JsonProperty(access = WRITE_ONLY)`.

---

### `PUT /usuario-service/usuarios/{id}`
Actualiza un usuario existente.

**Acceso:** `ADMINISTRADOR` (cualquier rol), `ADMINISTRATIVO` (solo `DOCENTE` o `ESTUDIANTE`)

**Request Body:** Similar a POST.

**Response 200:** Usuario actualizado.

---

### `DELETE /usuario-service/usuarios/{id}`
Elimina un usuario por su ID.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Response 200:**
```json
{
  "mensaje": "El usuario ha sido eliminado con éxito!"
}
```

---

### `GET /usuario-service/usuarios/{id}`
Obtiene un usuario por su ID.

**Acceso:** Todos los roles autenticados.

**Response 200:** Datos del usuario (sin campo `password`).

---

## Crypto (E2E Handshake)

### `GET /crypto/public-key`
Obtiene la clave pública RSA del servidor.

**Acceso:** Público

**Response 200:**
```json
{
  "n": "a1b2c3d4e5f6...",
  "e": "010001"
}
```

---

### `POST /crypto/handshake`
Intercambia una clave AES cifrada con RSA por un `sessionId`.

**Acceso:** Público

**Request Body:**
```json
{
  "encryptedAesKey": "<hex de la clave AES cifrada con RSA>"
}
```

**Response 200:**
```json
{
  "sessionId": "970e05a3-1d94-47fe-a273-c87c3b856443"
}
```

**Response 400:** Si el `encryptedAesKey` es inválido.

---

## Modelo de Datos

### `Usuario`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `codigo` | Long | ✅ (auto) | ID único del usuario |
| `nombre` | String | ✅ | Nombre (2-30 chars) |
| `apellido` | String | ✅ | Apellido (2-30 chars) |
| `email` | String | ✅ | Correo institucional (único) |
| `password` | String | ✅ | Contraseña (8+ chars, mayúscula, minúscula, número, especial) |
| `rol` | Enum | ✅ | `ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE` |
| `ultimoInicioSesion` | LocalDateTime | ❌ | Último login exitoso |

### `RolUsuario` (Enum)

```java
public enum RolUsuario {
    Estudiante,
    Docente,
    Administrativo,
    Administrador
}
```

