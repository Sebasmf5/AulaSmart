# Códigos de Error — reserva-service

> **Servicio:** reserva-service  
> **Puerto:** `8082`  
> **Base URL:** `http://<IP>:8082/api/v1/reserva-service`  
> **Estado E2E:** Desactivado (plano)  

---

## Errores de Negocio (Controlados)

### 400 Bad Request

#### `ValidationException` — Datos de entrada inválidos
Ocurre cuando el `@Valid` del controller detecta errores en los campos.

**Cuándo ocurre:**
- `aulaId` es nulo o vacío
- `horaInicio`/`horaFin` son nulas, están en el pasado, o `horaInicio >= horaFin`
- `titulo` está vacío o excede el límite

**Respuesta:**
```json
{
  "message": "Error de validacion de datos.",
  "error": [
    "El campo aulaId El ID del aula es obligatorio",
    "El campo titulo El motivo o título de la reserva es obligatorio",
    "El campo horaFin La hora de fin es obligatoria",
    "El campo horaInicio La hora de inicio es obligatoria"
  ]
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 400) {
  final errores = response.data['error'] as List<String>;
  showDialog(
    title: 'Datos incompletos',
    content: errores.join('\n'),
  );
}
```

---

#### `MethodArgumentNotValidException` — Validación de bean fallida
Similar al anterior, pero lanza Spring directamente antes de llegar al controller.

**Respuesta:**
```json
{
  "message": "Error de validación en los datos de la reserva.",
  "errores": [
    "aulaId: El ID del aula es obligatorio",
    "titulo: El motivo o título de la reserva es obligatorio"
  ],
  "status": 400
}
```

---

#### `HttpMessageNotReadableException` — JSON mal formado o tipos incorrectos
Ocurre cuando Jackson no puede convertir el JSON a la entidad `Reserva`.

**Cuándo ocurre:**
- Fechas en formato incorrecto (deben ser ISO: `2026-05-27T08:00:00`)
- Enum inválido (`estado: "ACTIVO"` en lugar de `CONFIRMADA`)
- Números enviados como string (`"aulaId": "abc"`)

**Respuesta:**
```json
{
  "message": "Error al leer los datos de la reserva. Verifica que los valores (estado, rol, fechas) sean válidos.",
  "detalle": "Cannot deserialize value of type java.time.LocalDateTime from String \"27/05/2026\": Failed to deserialize...",
  "status": 400
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 400 && response.data['detalle'] != null) {
  showSnackBar('Formato de fecha inválido. Use AAAA-MM-DDTHH:mm:ss');
}
```

---

#### `ResponseStatusException` — Restricción de negocio
Lanzado por el service cuando un estudiante intenta reservar un tipo de aula no permitido.

**Cuándo ocurre:**
- Estudiante intenta reservar aula que no sea tipo 78 o 79

**Respuesta:**
```json
{
  "message": "Los estudiantes solo pueden reservar aulas interactivas y audiovisuales",
  "status": 400
}
```

---

### 401 Unauthorized

#### Sin autenticación o JWT inválido
Ocurre **antes** de llegar al controller, en el filtro `JwtValidationFilter`.

**Cuándo ocurre:**
- No se envió header `Authorization`
- El token es inválido, expirado o mal formado
- El token no empieza con `Bearer `

**Respuesta:**
```json
{
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "status": 401
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 401) {
  // Redirigir a login
  Navigator.pushReplacementNamed('/login');
}
```

---

### 403 Forbidden

#### `AccesoNoAutorizadoException` — Rol insuficiente
Ocurre cuando un usuario intenta acceder a un endpoint que requiere rol de administrador.

**Cuándo ocurre:**
- `GET /reservas/pendientes` sin ser `ADMINISTRADOR`
- `PUT /reservas/{id}/confirmar` sin ser `ADMINISTRADOR`
- `PUT /reservas/{id}/rechazar` sin ser `ADMINISTRADOR`

**Respuesta:**
```json
{
  "message": "Esta operación requiere privilegios de administrador.",
  "status": 403
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 403) {
  showSnackBar('No tienes permisos para realizar esta acción');
}
```

---

### 404 Not Found

#### `ReservaNoEncontradaException` — Reserva no existe
Ocurre al buscar, actualizar, confirmar o eliminar una reserva con ID inexistente.

**Cuándo ocurre:**
- `GET /reservas/999` cuando la reserva 999 no existe
- `PUT /reservas` con `idReserva` inexistente
- `DELETE /reservas/{id}` con ID inexistente

**Respuesta:**
```json
{
  "message": "El producto con id 999 no existe.",
  "status": 404
}
```

**Nota:** El mensaje dice "producto" por un copy-paste del código. Trátalo como "reserva".

---

#### `PaginaSinReservasException` — Página vacía en paginación
Ocurre en `GET /reservas/page/{page}` cuando la página solicitada no tiene resultados.

**Respuesta:**
```json
{
  "message": "No hay reservas para esta pagina 99",
  "status": 404
}
```

---

#### `FeignException` (404) — Aula no encontrada
Ocurre cuando se consulta un `aulaId` que no existe en el `aula-service`.

**Respuesta:**
```json
{
  "message": "El aula consultada no existe en el sistema.",
  "status": 404
}
```

---

### 409 Conflict

#### `ReservaSolapadaException` — Horario ya ocupado
Ocurre cuando el rango de horas solicitado se solapa con otra reserva existente o con el sistema SIGA.

**Cuándo ocurre:**
- El aula ya está reservada en ese horario
- El aula está ocupada según el sistema SIGA universitario

**Respuesta:**
```json
{
  "message": "El horario que acabas solicitar se encuentra ya reservado",
  "status": 409
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 409) {
  showDialog(
    title: 'Aula no disponible',
    content: 'El horario seleccionado ya está ocupado. Por favor elige otro.',
  );
}
```

---

#### `ReservaModificadaException` — Conflicto de concurrencia (Optimistic Locking)
Ocurre cuando dos usuarios intentan modificar la misma reserva simultáneamente.

**Cuándo ocurre:**
- Usuario A carga la reserva (version: 0)
- Usuario B modifica la reserva (version cambia a 1)
- Usuario A intenta guardar con `version: 0` → falla

**Respuesta:**
```json
{
  "message": "La reserva con ID 5 fue modificada por otro usuario. Por favor recarga los datos e intenta de nuevo.",
  "status": 409
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 409 && response.data['message'].contains('modificada')) {
  showDialog(
    title: 'Datos desactualizados',
    content: 'Otro usuario modificó esta reserva. ¿Deseas recargar?',
    actions: [
      TextButton('Recargar', () => fetchReservaById(id)),
    ],
  );
}
```

---

#### `ReservaNoPermitidaException` — Modificación no permitida por estado
Ocurre al intentar modificar una reserva que ya está confirmada, cancelada o finalizada.

**Cuándo ocurre:**
- `PUT /reservas` con una reserva cuyo estado es `CONFIRMADA`

**Respuesta:**
```json
{
  "message": "No se puede modificar una reserva que ya está confirmada",
  "status": 409
}
```

---

### 200 OK (No es error, pero es un caso especial)

#### `NoHayReservasException` — Lista vacía
Ocurre en `GET /reservas` cuando no hay ninguna reserva en la base de datos.

**Respuesta:**
```json
{
  "message": "No hay reservas en la base de datos. ",
  "reserva": null
}
```

**Nota:** Devuelve HTTP 200 (no 404), porque no es un error del cliente. Es simplemente una lista vacía.

**Manejo en Flutter:**
```dart
if (response.statusCode == 200 && response.data['reserva'] == null) {
  showEmptyState('Aún no hay reservas registradas');
}
```

---

## Errores de Infraestructura / Inesperados

### 500 Internal Server Error

#### `Exception` genérica — Error del servidor
Cualquier excepción no manejada cae aquí. Incluye `ReservaExistenteException` (no tiene handler específico).

**Cuándo ocurre:**
- Error de base de datos (constraint violation)
- Error de red entre microservicios
- Bug no controlado

**Respuesta:**
```json
{
  "message": "Error interno del servidor al procesar la reserva.",
  "detalle": "could not execute statement [ERROR: duplicate key value violates unique constraint...]",
  "status": 500
}
```

**Manejo en Flutter:**
```dart
if (response.statusCode == 500) {
  showSnackBar('Error del servidor. Intenta más tarde.');
  // Opcional: loggear el detalle para debug
  print(response.data['detalle']);
}
```

---

## Resumen rápido para el Frontend

| Status | Caso | Mensaje al usuario |
|--------|------|-------------------|
| **200** | Lista vacía | "No hay reservas aún" |
| **400** | Datos inválidos | Mostrar lista de errores de validación |
| **401** | No autenticado | Redirigir a login |
| **403** | Sin permisos | "No tienes permisos para esto" |
| **404** | No encontrado | "La reserva/aula no existe" |
| **409** | Conflicto de negocio | Mostrar mensaje específico (ocupada, ya confirmada, etc.) |
| **500** | Error servidor | "Error del sistema. Intenta más tarde" |

---

## Ejemplo completo de handler en Flutter (Dio)

```dart
void handleReservaError(DioException e) {
  final response = e.response;
  if (response == null) {
    showSnackBar('Error de conexión. Verifica tu internet.');
    return;
  }

  final message = response.data['message'] ?? 'Error desconocido';

  switch (response.statusCode) {
    case 400:
      final errores = response.data['error'] ?? response.data['errores'];
      if (errores is List) {
        showDialog(title: 'Datos incorrectos', content: errores.join('\n'));
      } else {
        showSnackBar(message);
      }
      break;
    case 401:
      Navigator.pushReplacementNamed('/login');
      break;
    case 403:
      showSnackBar('No tienes permisos');
      break;
    case 404:
      showSnackBar('No encontrado: $message');
      break;
    case 409:
      showDialog(title: 'No se puede completar', content: message);
      break;
    case 500:
      showSnackBar('Error del servidor. Intenta más tarde.');
      break;
    default:
      showSnackBar('Error ${response.statusCode}: $message');
  }
}
```
