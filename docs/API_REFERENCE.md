# API Reference — AulaSmart Microservicios

> **Última actualización:** 2026-05-26
> **Estado E2E:** Solo `usuarios-service` tiene cifrado activo. `reserva-service`, `incidencia-service` y `chat-service` operan en **plano** (sin cifrado).

---

## 1. usuarios-service

**Puerto Host:** `8081`
**Base URL:** `http://<IP>:8081/api/v1`
**E2E:** ✅ Activo en `/usuario-service/*`. Requiere `x-session-id` si se usa cifrado.

### 1.1 Autenticación (siempre en plano)

#### `POST /auth/login`
Iniciar sesión y obtener JWT.

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "codigo": 12345,
  "password": "TuPassword123!"
}
```

**Respuesta 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
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

---

#### `POST /auth/refresh`
Refrescar token de acceso.

**Headers:**
```
Authorization: Bearer <refresh_token>
```

**Respuesta 200:** Misma estructura que login.

---

### 1.2 Crypto (HandShake E2E)

#### `GET /crypto/public-key`
Obtener clave pública RSA para cifrar la clave AES.

**Respuesta 200:**
```json
{
  "n": "a1b2c3...",
  "e": "010001"
}
```

---

#### `POST /crypto/handshake`
Intercambiar clave AES cifrada con RSA.

**Body:**
```json
{
  "encryptedAesKey": "<hex de la clave AES cifrada con RSA>"
}
```

**Respuesta 200:**
```json
{
  "sessionId": "970e05a3-1d94-47fe-a273-c87c3b856443"
}
```

---

### 1.3 Usuarios

#### `GET /usuario-service/usuarios`
Listar todos los usuarios.

**Headers:**
```
Authorization: Bearer <jwt_token>
x-session-id: <session_id>  // Solo si usas E2E
```

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Respuesta 200:**
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

#### `POST /usuario-service/usuarios`
Crear usuario.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
x-session-id: <session_id>  // Solo si usas E2E
```

**Roles:** `ADMINISTRADOR` (todos los roles), `ADMINISTRATIVO` (solo `DOCENTE`, `ESTUDIANTE`)

**Body:**
```json
{
  "nombre": "María",
  "apellido": "García",
  "email": "maria@uceva.edu.co",
  "password": "Password123!",
  "rol": "ESTUDIANTE"
}
```

**Respuesta 201:**
```json
{
  "mensaje": "El usuario ha sido creado con éxito!",
  "usuario": { ... }
}
```

**Respuesta 403:** Si un `ADMINISTRATIVO` intenta crear un `ADMINISTRADOR`.

---

#### `PUT /usuario-service/usuarios/{id}`
Actualizar usuario.

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO` (mismas reglas de creación)

**Body:** Similar a POST.

---

#### `DELETE /usuario-service/usuarios/{id}`
Eliminar usuario.

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

#### `GET /usuario-service/usuarios/{id}`
Buscar usuario por ID.

**Roles:** Todos los roles autenticados.

---

## 2. reserva-service

**Puerto Host:** `8082`
**Base URL:** `http://<IP>:8082/api/v1/reserva-service`
**E2E:** ❌ **Desactivado.** Peticiones en plano. NO enviar `x-session-id`.

### 2.1 Reservas

#### `GET /reservas`
Listar todas las reservas.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Roles:** Todos (autenticados)

---

#### `GET /reservas/{id}`
Obtener reserva por ID.

---

#### `POST /reservas`
Crear reserva.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Roles:** Todos (autenticados)

**Body:**
```json
{
  "aulaId": 195,
  "horaInicio": "2026-05-27T08:00:00",
  "horaFin": "2026-05-27T10:00:00",
  "titulo": "Clase de Matemáticas",
  "codigoPrograma": "ABC123",
  "grupo": "Grupo A"
}
```

**Nota:** `idSolicitante` y `rolSolicitante` se extraen del JWT automáticamente.

**Respuesta 201:**
```json
{
  "mensaje": "La reserva ha sido creada con éxito!",
  "reserva": { ... }
}
```

---

#### `PUT /reservas`
Modificar reserva.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO` (solo ellos pueden modificar)

**Body:**
```json
{
  "idReserva": 1,
  "version": 0,
  "aulaId": 195,
  "horaInicio": "2026-05-27T08:00:00",
  "horaFin": "2026-05-27T10:00:00",
  "titulo": "Clase actualizada"
}
```

**Restricción:** Solo se puede modificar si la reserva está en estado `PENDIENTE`.

**Respuesta 409:** Si intentas modificar una reserva `CONFIRMADA` o `CANCELADA`.

---

#### `DELETE /reservas/{id}`
Eliminar (cancelar) reserva.

**Roles:** Todos (autenticados)

**Nota:** Cambia el estado a `CANCELADA`, no borra de la BD.

---

#### `GET /reservas/mis-reservas`
Listar reservas del usuario autenticado.

---

#### `GET /reservas/usuario/{id}`
Listar reservas de un usuario específico.

---

### 2.2 Administración

#### `GET /reservas/pendientes`
Listar reservas pendientes de aprobación.

**Roles:** `ADMINISTRADOR`

---

#### `PUT /reservas/{id}/confirmar`
Confirmar reserva pendiente.

**Roles:** `ADMINISTRADOR`

---

#### `PUT /reservas/{id}/rechazar`
Rechazar (cancelar) reserva pendiente.

**Roles:** `ADMINISTRADOR`

---

#### `GET /reservas/ocupadas`
Consultar aulas ocupadas en un rango.

**Params:** `fecha=2026-05-27`, `horaInicio=08:00`, `horaFin=10:00`

---

## 3. aula-service

**Puerto Host:** `8083`
**Base URL:** `http://<IP>:8083/api/v1/aula-service`
**E2E:** ❌ **No implementado.**

### 3.1 Aulas

#### `GET /aulas`
Listar todas las aulas.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

---

#### `GET /aulas/{id}`
Obtener aula por ID.

---

## 4. chat-service

**Puerto Host:** `8086`
**Base URL:** `http://<IP>:8086/api/v1/chat`
**E2E:** ❌ **Desactivado.** Peticiones en plano. NO enviar `x-session-id`.

### 4.1 Chatbot

#### `POST /chat`
Enviar mensaje al chatbot (IA).

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Body:**
```json
{
  "message": "Quiero reservar el aula 195 mañana a las 8am"
}
```

**Respuesta 200:**
```json
{
  "respuesta": "He creado tu reserva para el aula 195...",
  "accion": "RESERVA_CREADA",
  "datos": { ... }
}
```

---

## 5. incidencia-service

**Puerto Host:** `8085`
**Base URL:** `http://<IP>:8085/api/v1/incidencia-service`
**E2E:** ❌ **Desactivado.** Peticiones en plano. NO enviar `x-session-id`.

### 5.1 Incidencias

#### `POST /incidencias`
Crear incidencia.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`

**Body (solo 3 campos):**
```json
{
  "codigoAula": 123,
  "descripcionBreve": "El proyector no enciende y presenta manchas en la lente",
  "tipoIncidencia": "HARDWARE"
}
```

**Nota:** No enviar `codigoUsuario`, `estado`, `cartaFormalGenerada`, `fechaReporte`, etc. El backend los maneja automáticamente.

**Respuesta 201:**
```json
{
  "mensaje": "La incidencia se ha reportado y guardado con éxito!",
  "incidencia": {
    "id": 1,
    "codigoAula": 123,
    "codigoUsuario": 2,
    "descripcionBreve": "El proyector no enciende...",
    "tipoIncidencia": "HARDWARE",
    "estado": "PENDIENTE",
    "cartaFormalGenerada": "<p>Por medio de la presente...</p>",
    "fechaReporte": "2026-05-26T10:30:00",
    "urlImagen": null
  }
}
```

---

#### `POST /incidencias/{id}/imagen`
Subir imagen de evidencia.

**Headers:**
```
Content-Type: multipart/form-data
Authorization: Bearer <jwt_token>
```

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`

**Body:** `multipart/form-data` con campo `imagen` (tipo File)

**Respuesta 200:**
```json
{
  "mensaje": "Imagen de evidencia subida exitosamente.",
  "incidencia": {
    "id": 1,
    "urlImagen": "incidencia_1_a0be1608.jpg"
  }
}
```

---

#### `GET /incidencias/{id}/imagen`
Ver imagen de evidencia.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Roles:** Todos (autenticados)

**Respuesta 200:** Devuelve el archivo de imagen con `Content-Type: image/jpeg` (o png).

**Uso en Flutter:**
```dart
Image.network(
  'http://<IP>:8085/api/v1/incidencia-service/incidencias/1/imagen',
  headers: {'Authorization': 'Bearer $jwtToken'},
)
```

---

#### `GET /incidencias`
Listar todas las incidencias.

**Roles:** Todos (autenticados)

---

#### `GET /incidencias/{id}`
Obtener incidencia por ID.

---

#### `PUT /incidencias/{id}`
Actualizar incidencia.

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

#### `DELETE /incidencias/{id}`
Eliminar incidencia.

**Roles:** `ADMINISTRADOR`

---

### 5.2 Panel Administrativo

#### `GET /incidencias/pendientes`
Listar incidencias pendientes.

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

#### `GET /incidencias/pendientes/count`
Contar incidencias pendientes (para badge).

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Respuesta 200:**
```json
{
  "cantidad": 5
}
```

---

#### `PUT /incidencias/{id}/responder`
Responder una incidencia.

**Roles:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Body:**
```json
{
  "respuesta": "Se ha enviado al área de mantenimiento. Estará resuelto en 48 horas."
}
```

**Respuesta 200:**
```json
{
  "mensaje": "La incidencia ha sido respondida exitosamente.",
  "incidencia": {
    "id": 1,
    "estado": "REVISADA",
    "respuestaAdministracion": "Se ha enviado al área de mantenimiento...",
    "fechaRespuesta": "2026-05-26T14:00:00",
    "codigoAdministrador": 2
  }
}
```

---

## Resumen de E2E por Servicio

| Servicio | Puerto | ¿E2E Activo? | ¿Enviar `x-session-id`? |
|----------|--------|--------------|------------------------|
| `usuarios-service` | `8081` | ✅ Sí (en `/usuario-service/*`) | Opcional (si se hace handshake) |
| `reserva-service` | `8082` | ❌ No | **NO** |
| `aula-service` | `8083` | ❌ No implementado | **NO** |
| `chat-service` | `8086` | ❌ No | **NO** |
| `incidencia-service` | `8085` | ❌ No | **NO** |

---

## Errores comunes

### 400 Bad Request — "Error de validacion de datos"
- El body tiene campos vacíos o tipos incorrectos
- Verificar que se envían los campos exactos que pide el DTO

### 401 Unauthorized
- Falta header `Authorization` o el JWT es inválido/expirado

### 403 Forbidden
- El usuario no tiene el rol requerido para esa operación

### 409 Conflict
- Reserva solapada con otra existente
- Intento de modificar una reserva que ya está confirmada/cancelada

### 500 Server Error
- Error interno del servidor (ver logs del backend)
- Problemas de permisos en disco (para subida de imágenes)
