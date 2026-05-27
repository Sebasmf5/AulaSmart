# API Reference: `incidencia-service`

> **Puerto:** `8085`  
> **URL Base:** `http://<host>:8085/api/v1/incidencia-service`  
> **E2E:** Desactivado. Peticiones en plano.  

---

## Incidencias

### `POST /incidencias`
Reporta una nueva incidencia. El sistema genera automáticamente una carta formal mediante IA.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Request Body (solo 3 campos):**
```json
{
  "codigoAula": 123,
  "descripcionBreve": "El proyector no enciende y presenta manchas en la lente",
  "tipoIncidencia": "HARDWARE"
}
```

**Campos gestionados automáticamente por el backend (no enviar):**
- `codigoUsuario` → extraído del JWT
- `estado` → `PENDIENTE` por defecto
- `fechaReporte` → generada automáticamente
- `cartaFormalGenerada` → generada por IA (`ChatGPTCartaService`)

**Response 201:**
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

### `POST /incidencias/{id}/imagen`
Sube una imagen de evidencia a una incidencia existente.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`

**Headers:**
```
Content-Type: multipart/form-data
Authorization: Bearer <jwt_token>
```

**Body:** `multipart/form-data` con campo `imagen` (tipo `File`).

**Response 200:**
```json
{
  "mensaje": "Imagen de evidencia subida exitosamente.",
  "incidencia": {
    "id": 1,
    "urlImagen": "incidencia_1_a0be1608.jpg"
  }
}
```

**Errores comunes:**
- `500`: Error de permisos en directorio de uploads (verificar volumen Docker).

---

### `GET /incidencias/{id}/imagen`
Obtiene la imagen de evidencia de una incidencia.

**Acceso:** Todos los roles autenticados.

**Response 200:** Archivo de imagen con `Content-Type: image/jpeg` (o PNG).

**Uso en Flutter:**
```dart
Image.network(
  'http://<host>:8085/api/v1/incidencia-service/incidencias/1/imagen',
  headers: {'Authorization': 'Bearer $jwtToken'},
)
```

---

### `GET /incidencias`
Lista todas las incidencias.

**Acceso:** Todos los roles autenticados.

---

### `GET /incidencias/page/{page}`
Lista incidencias paginadas (tamaño de página: 4).

**Acceso:** Todos los roles autenticados.

---

### `GET /incidencias/{id}`
Obtiene una incidencia por su ID.

**Acceso:** Todos los roles autenticados.

---

### `PUT /incidencias/{id}`
Actualiza una incidencia existente.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Nota:** Preserva `codigoUsuario` y `fechaReporte` originales.

---

### `DELETE /incidencias/{id}`
Elimina una incidencia.

**Acceso:** `ADMINISTRADOR`

---

## Panel Administrativo

### `GET /incidencias/pendientes`
Lista incidencias pendientes de respuesta.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

---

### `GET /incidencias/pendientes/count`
Devuelve la cantidad de incidencias pendientes (útil para badges/notificaciones).

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Response 200:**
```json
{
  "cantidad": 5
}
```

---

### `PUT /incidencias/{id}/responder`
Responde una incidencia y cambia su estado a `REVISADA`.

**Acceso:** `ADMINISTRADOR`, `ADMINISTRATIVO`

**Request Body:**
```json
{
  "respuesta": "Se ha enviado al área de mantenimiento. Estará resuelto en 48 horas."
}
```

**Response 200:**
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

## Modelo de Datos

### `Incidencia`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `id` | Long | ✅ (auto) | ID único |
| `codigoAula` | Long | ✅ | ID del aula afectada |
| `codigoUsuario` | Long | ✅ (JWT) | ID del usuario que reporta |
| `descripcionBreve` | String | ✅ | Descripción del problema (max 500) |
| `urlImagen` | String | ❌ | Nombre del archivo de evidencia |
| `cartaFormalGenerada` | String | ❌ (auto) | HTML/texto de la carta generada por IA |
| `tipoIncidencia` | Enum | ✅ | `HARDWARE`, `SOFTWARE`, `INFRAESTRUCTURA`, `OTRO` |
| `estado` | Enum | ✅ (auto) | `PENDIENTE`, `REVISADA`, `CERRADA` |
| `respuestaAdministracion` | String | ❌ | Respuesta del administrador |
| `fechaRespuesta` | LocalDateTime | ❌ (auto) | Fecha de respuesta |
| `codigoAdministrador` | Long | ❌ | ID del admin que respondió |
| `fechaReporte` | LocalDateTime | ✅ (auto) | Fecha de creación |

### `TipoIncidencia` (Enum)

```java
public enum TipoIncidencia {
    HARDWARE,
    SOFTWARE,
    INFRAESTRUCTURA,
    OTRO
}
```

### `EstadoIncidencia` (Enum)

```java
public enum EstadoIncidencia {
    PENDIENTE,
    REVISADA,
    CERRADA
}
```

---

## Flujo de Negocio

```
1. USUARIO reporta incidencia → POST /incidencias
   └── IA genera carta formal automáticamente

2. SISTEMA guarda incidencia con estado PENDIENTE

3. ADMIN ve badge de pendientes → GET /incidencias/pendientes/count

4. ADMIN revisa detalle y responde → PUT /{id}/responder
   └── Estado cambia a REVISADA

5. USUARIO ve respuesta en su lista → GET /incidencias
```

