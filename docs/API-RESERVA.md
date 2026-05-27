# API Reference: `reserva-service`

> **Puerto:** `8082`  
> **URL Base:** `http://<host>:8082/api/v1/reserva-service`  
> **E2E:** Desactivado. Peticiones en plano. No enviar `x-session-id`.  

---

## Reservas

### `GET /reservas`
Lista todas las reservas registradas.

**Acceso:** Todos los usuarios autenticados.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Response 200:**
```json
{
  "reservas": [
    {
      "idReserva": 1,
      "aulaId": 195,
      "horaInicio": "2026-05-27T08:00:00",
      "horaFin": "2026-05-27T10:00:00",
      "estado": "CONFIRMADA",
      "idSolicitante": 12345,
      "rolSolicitante": "DOCENTE",
      "nombreUsuarioResponsable": "Juan Pérez",
      "titulo": "Clase de Matemáticas"
    }
  ]
}
```

---

### `GET /reservas/{id}`
Obtiene una reserva por su ID.

**Acceso:** Todos los usuarios autenticados.

---

### `GET /reservas/page/{page}`
Lista reservas paginadas (tamaño de página: 4).

**Acceso:** Todos los usuarios autenticados.

---

### `POST /reservas`
Crea una nueva reserva.

**Acceso:** Todos los usuarios autenticados.

**Request Body:**
```json
{
  "aulaId": 195,
  "horaInicio": "2026-05-27T08:00:00",
  "horaFin": "2026-05-27T10:00:00",
  "titulo": "Clase de Matemáticas",
  "codigoPrograma": "ING-SIST",
  "grupo": "Grupo A"
}
```

**Notas:**
- `idSolicitante`, `rolSolicitante` y `nombreUsuarioResponsable` se extraen automáticamente del JWT.
- Si el aula requiere autorización, el estado será `PENDIENTE`; de lo contrario, `CONFIRMADA`.
- Los estudiantes solo pueden reservar aulas de tipo `78` o `79`.

**Response 201:**
```json
{
  "mensaje": "La reserva ha sido creada con éxito!",
  "reserva": { ... }
}
```

**Errores comunes:**
- `409 Conflict`: Horario solapado con otra reserva o SIGA.
- `409 Conflict`: Estudiante intenta reservar aula no permitida.

---

### `PUT /reservas`
Actualiza una reserva existente.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO` (exclusivo)

**Request Body:**
```json
{
  "idReserva": 1,
  "version": 0,
  "aulaId": 195,
  "horaInicio": "2026-05-27T09:00:00",
  "horaFin": "2026-05-27T11:00:00",
  "titulo": "Clase actualizada"
}
```

**Restricciones:**
- Solo se puede modificar si la reserva está en estado `PENDIENTE`.
- El campo `version` es obligatorio (optimistic locking).

**Response 409:** Si la reserva ya está `CONFIRMADA`:
```json
{
  "message": "No se puede modificar una reserva que ya está confirmada",
  "status": 409
}
```

---

### `DELETE /reservas/{id}`
Elimina (cancela) una reserva.

**Acceso:** Todos los usuarios autenticados.

**Nota:** Cambia el estado a `CANCELADA`; no elimina el registro físico.

---

### `GET /reservas/mis-reservas`
Lista las reservas del usuario autenticado.

**Acceso:** Todos los usuarios autenticados.

---

### `GET /reservas/usuario/{id}`
Lista las reservas de un usuario específico.

**Acceso:** Todos los usuarios autenticados.

---

### `GET /reservas/aula/{aulaId}/agregadas`
Lista reservas unificadas (AulaSmart + SIGA) para un aula.

**Acceso:** Todos los usuarios autenticados.

---

### `GET /reservas/ocupadas`
Consulta aulas ocupadas en un rango de fecha/hora.

**Acceso:** Todos los usuarios autenticados.

**Query Params:**
- `fecha`: `2026-05-27`
- `horaInicio`: `08:00`
- `horaFin`: `10:00`

**Response 200:** `List<Long>` (IDs de aulas ocupadas)

---

## Panel Administrativo

### `GET /reservas/pendientes`
Lista reservas pendientes de aprobación.

**Acceso:** `ADMINISTRADOR`

---

### `PUT /reservas/{id}/confirmar`
Confirma una reserva pendiente.

**Acceso:** `ADMINISTRADOR`

**Response 200:**
```json
{
  "mensaje": "La reserva ha sido confirmada con éxito.",
  "reserva": { ... }
}
```

---

### `PUT /reservas/{id}/rechazar`
Rechaza una reserva pendiente.

**Acceso:** `ADMINISTRADOR`

**Response 200:**
```json
{
  "mensaje": "La reserva ha sido rechazada con éxito.",
  "reserva": { ... }
}
```

---

## Modelo de Datos

### `Reserva`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `idReserva` | Long | ✅ (auto) | ID único |
| `aulaId` | Long | ✅ | ID del aula |
| `horaInicio` | LocalDateTime | ✅ | Inicio de la reserva |
| `horaFin` | LocalDateTime | ✅ | Fin de la reserva |
| `estado` | Enum | ✅ | `PENDIENTE`, `CONFIRMADA`, `CANCELADA`, `FINALIZADA` |
| `idSolicitante` | Long | ✅ | ID del usuario (JWT) |
| `rolSolicitante` | Enum | ✅ | Rol del usuario (JWT) |
| `nombreUsuarioResponsable` | String | ❌ | Nombre del responsable |
| `codigoPrograma` | String | ❌ | Programa académico |
| `grupo` | String | ❌ | Grupo o sección |
| `titulo` | String | ✅ | Motivo de la reserva |
| `version` | Long | ✅ (PUT) | Versión para optimistic locking |

### `EstadosReserva` (Enum)

```java
public enum EstadosReserva {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    FINALIZADA
}
```

---

## Códigos de Error

Ver documento completo: [`RESERVA_ERROR_CODES.md`](./RESERVA_ERROR_CODES.md)

| HTTP | Significado | Ejemplo |
|------|-------------|---------|
| `200` | OK, pero lista vacía | `NoHayReservasException` |
| `400` | Datos inválidos | Campos vacíos, fechas mal formateadas |
| `401` | No autenticado | Falta JWT |
| `403` | Sin permisos | No es admin y quiere confirmar reserva |
| `404` | No encontrado | Reserva o aula inexistente |
| `409` | Conflicto de negocio | Horario ocupado, reserva ya confirmada, optimistic locking |
| `500` | Error interno | Fallo de base de datos |

