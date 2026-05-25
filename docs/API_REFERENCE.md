# Lista Completa de APIs — AulaSmart

> Documento generado automáticamente desde el código fuente de los microservicios.

---

## 1. usuarios-service (Puerto 8081)

Base URL: `http://usuarios-service:8081`

### Autenticación (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/login` | Login con código y password | ❌ Público |
| POST | `/api/v1/auth/register` | Registro de usuario | ❌ Público |
| POST | `/api/v1/auth/refresh` | Refrescar token JWT | ❌ Público |
| POST | `/api/v1/auth/logout` | Cerrar sesión | ✅ JWT |

### Crypto (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/crypto/public-key` | Obtener clave pública RSA | ❌ Público |
| POST | `/api/v1/crypto/handshake` | Crear sesión criptográfica | ❌ Público |

### Usuarios (Protegido)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/usuario-service/usuarios` | Listar todos los usuarios | ADMINISTRADOR, ADMINISTRATIVO |
| GET | `/api/v1/usuario-service/usuarios/page/{page}` | Listar usuarios paginados | ADMINISTRADOR, ADMINISTRATIVO |
| POST | `/api/v1/usuario-service/usuarios` | Crear nuevo usuario | ADMINISTRADOR, ADMINISTRATIVO |
| GET | `/api/v1/usuario-service/usuarios/{id}` | Obtener usuario por ID | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| PUT | `/api/v1/usuario-service/usuarios/{id}` | Actualizar usuario | ADMINISTRADOR, ADMINISTRATIVO |
| DELETE | `/api/v1/usuario-service/usuarios/{id}` | Eliminar usuario | ADMINISTRADOR, ADMINISTRATIVO |

---

## 2. aula-service (Puerto 8083)

Base URL: `http://aula-service:8083`

### Crypto (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/crypto/public-key` | Obtener clave pública RSA | ❌ Público |
| POST | `/api/v1/crypto/handshake` | Crear sesión criptográfica | ❌ Público |

### Aulas (Protegido)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/aula-service/aulas` | Listar todas las aulas | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aula/page/{page}` | Listar aulas paginadas | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| POST | `/api/v1/aula-service/aulas` | Crear nueva aula | ADMINISTRADOR, ADMINISTRATIVO |
| GET | `/api/v1/aula-service/aulas/{id}` | Obtener aula por ID | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/codigo/{codigo}` | Obtener aula por código SIGA | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| PUT | `/api/v1/aula-service/aulas` | Actualizar aula | ADMINISTRADOR, ADMINISTRATIVO |
| DELETE | `/api/v1/aula-service/aulas/{id}` | Eliminar aula | ADMINISTRADOR, ADMINISTRATIVO |
| GET | `/api/v1/aula-service/aulas/bloque/{bloqueId}` | Listar aulas por bloque | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/facultad/{facultadId}` | Listar aulas por facultad | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/buscar/{nombreAula}` | Buscar aulas por nombre | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/tipo-aula/{tipoAula}` | Listar aulas por tipo | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/codigos` | Listar todos los códigos SIGA | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/codigos-siga` | Listar códigos SIGA (solo sincronizadas) | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/aula-service/aulas/sincronizadas-siga` | Listar aulas sincronizadas con SIGA | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |

### Endpoints Internos (Uso entre microservicios)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/aula-service/aulas/{id}/tipo` | Obtener código de tipo de aula por ID |
| GET | `/api/v1/aula-service/aulas/{id}/requiere-autorizacion` | Verificar si requiere autorización |
| GET | `/api/v1/aula-service/aulas/{id}/codigo-siga` | Obtener código SIGA por ID |
| GET | `/api/v1/aula-service/aulas/tipo/{codigo}` | Obtener tipo de aula por código |
| GET | `/api/v1/aula-service/aulas/siga/{codigo}` | Obtener pasaporte SIGA |
| GET | `/api/v1/aula-service/aulas/requiere-autorizacion/{codigo}` | Verificar autorización por código |

### Bloques

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/aula-service/bloques` | Listar todos los bloques |
| GET | `/api/v1/aula-service/bloques/facultad/{facultadId}` | Listar bloques por facultad |
| GET | `/api/v1/aula-service/bloques/buscar/{nombre}` | Buscar bloque por nombre |

### Facultades

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/aula-service/facultades` | Listar todas las facultades |

### Tipos de Aula

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/aula-service/tipos-aula` | Listar todos los tipos de aula |

---

## 3. reserva-service (Puerto 8082)

Base URL: `http://reserva-service:8082`

### Crypto (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/crypto/public-key` | Obtener clave pública RSA | ❌ Público |
| POST | `/api/v1/crypto/handshake` | Crear sesión criptográfica | ❌ Público |

### Reservas (Protegido)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/reserva-service/reservas` | Listar todas las reservas | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/page/{page}` | Listar reservas paginadas | JWT requerido |
| POST | `/api/v1/reserva-service/reservas` | Crear nueva reserva | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/{id}` | Obtener reserva por ID | JWT requerido |
| PUT | `/api/v1/reserva-service/reservas` | Actualizar reserva | JWT requerido |
| DELETE | `/api/v1/reserva-service/reservas` | Eliminar reserva | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/mis-reservas` | Listar reservas del usuario autenticado | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/usuario/{id}` | Listar reservas de un usuario específico | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/aula/{aulaId}/agregadas` | Listar reservas agregadas (AulaSmart + SIGA) | JWT requerido |
| GET | `/api/v1/reserva-service/reservas/ocupadas` | Obtener IDs de aulas ocupadas en rango | JWT requerido |

### Admin Reservas (Solo Administrador)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/reserva-service/reservas/pendientes` | Listar reservas pendientes | ADMINISTRADOR |
| PUT | `/api/v1/reserva-service/reservas/{id}/confirmar` | Confirmar reserva pendiente | ADMINISTRADOR |
| PUT | `/api/v1/reserva-service/reservas/{id}/rechazar` | Rechazar reserva pendiente | ADMINISTRADOR |

---

## 4. chat-service (Puerto 8086)

Base URL: `http://chat-service:8086`

### Crypto (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/crypto/public-key` | Obtener clave pública RSA | ❌ Público |
| POST | `/api/v1/crypto/handshake` | Crear sesión criptográfica | ❌ Público |

### Chat (Protegido con JWT)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/chat` | Enviar mensaje al chatbot | ✅ JWT |
| POST | `/api/v1/chat/reset` | Reiniciar conversación | ✅ JWT |

---

## 5. incidencia-service (Puerto 8085)

Base URL: `http://incidencia-service:8085`

### Crypto (Público)

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/crypto/public-key` | Obtener clave pública RSA | ❌ Público |
| POST | `/api/v1/crypto/handshake` | Crear sesión criptográfica | ❌ Público |

### Incidencias (Protegido)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/incidencia-service/incidencias` | Listar todas las incidencias | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/incidencia-service/incidencias/page/{page}` | Listar incidencias paginadas | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| POST | `/api/v1/incidencia-service/incidencias` | Crear nueva incidencia | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| GET | `/api/v1/incidencia-service/incidencias/{id}` | Obtener incidencia por ID | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |
| PUT | `/api/v1/incidencia-service/incidencias/{id}` | Actualizar incidencia | ADMINISTRADOR, ADMINISTRATIVO |
| DELETE | `/api/v1/incidencia-service/incidencias/{id}` | Eliminar incidencia | ADMINISTRADOR |
| POST | `/api/v1/incidencia-service/incidencias/{id}/imagen` | Subir imagen de evidencia | ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE |

### Admin Incidencias (Solo Administrador)

| Método | Endpoint | Descripción | Roles |
|--------|----------|-------------|-------|
| GET | `/api/v1/incidencia-service/incidencias/pendientes` | Listar incidencias pendientes | ADMINISTRADOR, ADMINISTRATIVO |
| GET | `/api/v1/incidencia-service/incidencias/pendientes/count` | Contar incidencias pendientes (badge) | ADMINISTRADOR, ADMINISTRATIVO |
| PUT | `/api/v1/incidencia-service/incidencias/{id}/responder` | Responder incidencia | ADMINISTRADOR, ADMINISTRATIVO |

---

## Resumen de Puertos

| Servicio | Puerto Interno | Puerto Externo (Docker) |
|----------|----------------|------------------------|
| usuarios-service | 8081 | 8081 |
| aula-service | 8082 | 8083 |
| reserva-service | 8080 | 8082 |
| chat-service | 8086 | 8086 |
| incidencia-service | 8087 | 8085 |

## Notas

- **Endpoints públicos** (`/auth/*`, `/crypto/*`) no requieren JWT ni E2E.
- **Endpoints protegidos** requieren `Authorization: Bearer <token>` en el header.
- **E2E opcional**: Si no se envía `x-session-id`, el backend procesa la petición en plano (útil para testing).
- **E2E activo**: Si se envía `x-session-id` válido, el body debe ir cifrado con AES.

---

*Documento generado desde el código fuente de AulaSmart.*
