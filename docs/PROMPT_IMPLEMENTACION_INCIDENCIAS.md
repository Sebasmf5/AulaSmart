# Prompt de Implementación — Módulo de Incidencias (Flutter)

> **Instrucciones para el agente de IA:** Implementa el módulo completo de incidencias en la app Flutter de AulaSmart. Sigue TODAS las especificaciones técnicas y de diseño que se detallan a continuación. No omitas ningún punto.

---

## 1. Contexto del Sistema

AulaSmart es una app universitaria con arquitectura de microservicios. El backend ya está completo y funcional. Tu trabajo es construir el **frontend del módulo de incidencias**.

### Flujo de negocio

1. **Usuario** (docente/estudiante/admin) reporta un daño o problema en un aula
2. El sistema **genera automáticamente una carta formal** usando IA (ChatGPT) en el backend
3. El **administrador** revisa la incidencia, puede responder y cambiar su estado
4. El **usuario** ve el estado y la respuesta de la administración

---

## 2. APIs del Backend (ya disponibles)

Base URL: `http://incidencia-service:8085/api/v1/incidencia-service`

### Endpoints

| Método | Endpoint | Body/Params | Respuesta |
|--------|----------|-------------|-----------|
| `POST` | `/incidencias` | JSON: `{"codigoAula": 123, "descripcionBreve": "texto", "tipoIncidencia": "HARDWARE"}` | Incidencia creada (con `cartaFormalGenerada` incluida) |
| `POST` | `/incidencias/{id}/imagen` | `multipart/form-data`: campo `imagen` (File) | Incidencia con `urlImagen` actualizada |
| `GET` | `/incidencias` | — | Lista de incidencias |
| `GET` | `/incidencias/page/{page}` | — | Lista paginada |
| `GET` | `/incidencias/{id}` | — | Incidencia por ID |
| `PUT` | `/incidencias/{id}` | JSON con campos a actualizar | Incidencia actualizada |
| `DELETE` | `/incidencias/{id}` | — | 200 OK |
| `GET` | `/incidencias/pendientes` | — | Lista de incidencias pendientes (solo admin) |
| `GET` | `/incidencias/pendientes/count` | — | `{"cantidad": 5}` (solo admin) |
| `PUT` | `/incidencias/{id}/responder` | JSON: `{"respuesta": "texto de respuesta"}` | Incidencia con estado `REVISADA` (solo admin) |

### Headers requeridos

```
Content-Type: application/json
Authorization: Bearer <jwt_token>
x-session-id: <session_id>  // Solo si el interceptor E2E ya está configurado
```

> **Nota E2E:** Si el interceptor criptográfico (`CryptoInterceptor`) ya está implementado, no modifiques nada. Si NO está implementado, asume que las peticiones van en plano por ahora.

---

## 3. Modelo de Datos (Flutter)

```dart
enum TipoIncidencia {
  HARDWARE,
  SOFTWARE,
  INFRAESTRUCTURA,
  OTRO,
}

enum EstadoIncidencia {
  PENDIENTE,    // Carta creada, esperando respuesta
  REVISADA,     // El administrador respondió
  CERRADA,      // Caso resuelto
}

class Incidencia {
  final int? id;
  final int codigoAula;
  final int? codigoUsuario;       // Se extrae del JWT, no enviar desde frontend
  final String descripcionBreve;
  final String? urlImagen;        // Nombre del archivo, ej: "incidencia_1_a3f8b2d1.jpg"
  final String? cartaFormalGenerada; // HTML o texto plano de la carta
  final TipoIncidencia tipoIncidencia;
  final EstadoIncidencia estado;
  final String? respuestaAdministracion;
  final DateTime? fechaRespuesta;
  final int? codigoAdministrador;
  final DateTime? fechaReporte;

  Incidencia({...});
  factory Incidencia.fromJson(Map<String, dynamic> json) {...}
  Map<String, dynamic> toJson() {...}
}
```

---

## 4. Requisitos Funcionales

### 4.1 Para usuarios (Docente/Estudiante/Admin)

- [ ] **Crear incidencia**: Flujo de DOS PASOS obligatorio:

  **PASO 1 — Crear la incidencia (JSON)**
  Enviar `POST /api/v1/incidencia-service/incidencias` con **SOLO estos 3 campos**:
  ```json
  {
    "codigoAula": 123,
    "descripcionBreve": "El proyector no enciende",
    "tipoIncidencia": "HARDWARE"
  }
  ```
  **NO enviar**: `codigoUsuario`, `estado`, `cartaFormalGenerada`, `fechaReporte`, `urlImagen`, `respuestaAdministracion`, `fechaRespuesta`, `codigoAdministrador`. El backend los ignora o los genera automáticamente.

  La respuesta trae la incidencia completa incluyendo `cartaFormalGenerada` generada por IA.

  **PASO 2 — Subir imagen (solo si el usuario seleccionó una)**
  Si hay imagen, hacer `POST /api/v1/incidencia-service/incidencias/{id}/imagen` con `multipart/form-data`.

  **Por qué en 2 pasos**: El endpoint JSON (`/incidencias`) no acepta imágenes binarias. Además, si la imagen falla, la incidencia ya quedó creada con su carta.

  UI sugerida:
  - Formulario con selector de aula, tipo, descripción (max 500 chars, contador) y adjuntar imagen (opcional, preview local).
  - Botón "Generar Carta y Enviar" (loading indicator).
  - BottomSheet mostrando la `cartaFormalGenerada` devuelta por el backend.
  - Botón "Confirmar envío" en el BottomSheet. Si había imagen seleccionada, subirla ahora.
- [ ] **Ver mis incidencias**: Lista con tarjetas mostrando estado, fecha, aula y tipo
- [ ] **Ver detalle**: Pantalla completa con:
  - Descripción
  - Carta formal generada (renderizar HTML si aplica, o texto plano)
  - Imagen de evidencia (si existe) — cargar desde `http://incidencia-service:8085/app/uploads/incidencias/{urlImagen}`
  - Respuesta de administración (si estado es `REVISADA` o `CERRADA`)
  - Fecha de respuesta
- [ ] **Eliminar mi incidencia**: Solo si es `PENDIENTE` y fue creada por el usuario logueado

### 4.2 Para administradores (rol ADMINISTRADOR o ADMINISTRATIVO)

- [ ] **Panel de incidencias pendientes**:
  - Lista priorizada (las más antiguas primero)
  - Badge con contador de pendientes (obtenido de `/incidencias/pendientes/count`)
  - Indicador visual de urgencia (color rojo si lleva > 7 días en PENDIENTE)
- [ ] **Responder incidencia**:
  - Pantalla con detalle completo
  - Campo de texto para respuesta (máx 2000 caracteres)
  - Botón "Marcar como revisada" que envía respuesta
  - Botón "Cerrar caso" (opcional, cambia estado a CERRADA)
- [ ] **Notificaciones**: Badge en el BottomNavigationBar cuando hay pendientes

---

## 5. Diseño UI/UX (Material 3)

### Paleta de colores por estado

| Estado | Color | Icono |
|--------|-------|-------|
| `PENDIENTE` | `Colors.orange` | `Icons.hourglass_top` |
| `REVISADA` | `Colors.blue` | `Icons.check_circle_outline` |
| `CERRADA` | `Colors.green` | `Icons.verified` |

### Paleta de colores por tipo de incidencia

| Tipo | Color sugerido | Icono sugerido |
|------|----------------|----------------|
| `HARDWARE` | `Colors.red.shade100` (chip) / `Colors.red` (texto) | `Icons.computer` |
| `SOFTWARE` | `Colors.blue.shade100` (chip) / `Colors.blue` (texto) | `Icons.code` |
| `INFRAESTRUCTURA` | `Colors.amber.shade100` (chip) / `Colors.orange` (texto) | `Icons.account_balance` |
| `OTRO` | `Colors.grey.shade300` (chip) / `Colors.grey` (texto) | `Icons.help_outline` |

### Tarjeta de incidencia (IncidenciaCard)

```dart
Card(
  elevation: 2,
  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
  child: Padding(
    padding: EdgeInsets.all(16),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Chip(
              label: Text(tipoIncidencia.name),
              backgroundColor: _getTipoColor(tipoIncidencia),
            ),
            Spacer(),
            Chip(
              label: Text(estado.name),
              backgroundColor: _getEstadoColor(estado),
            ),
          ],
        ),
        SizedBox(height: 8),
        Text('Aula: $codigoAula', style: TextStyle(fontWeight: FontWeight.bold)),
        SizedBox(height: 4),
        Text(descripcionBreve, maxLines: 2, overflow: TextOverflow.ellipsis),
        SizedBox(height: 8),
        Row(
          children: [
            Icon(Icons.calendar_today, size: 14, color: Colors.grey),
            SizedBox(width: 4),
            Text(DateFormat('dd/MM/yyyy').format(fechaReporte), style: TextStyle(color: Colors.grey)),
            if (urlImagen != null) ...[
              Spacer(),
              Icon(Icons.image, size: 14, color: Colors.blue),
            ],
          ],
        ),
      ],
    ),
  ),
)
```

### Pantalla de creación (CreateIncidenciaScreen)

- AppBar con título "Reportar Incidencia"
- Formulario con validación
- Dropdown para seleccionar aula (obtenida de `aula-service`)
- Dropdown para tipo de incidencia
- TextField multilínea para descripción (max 500 chars, contador)
- Botón para adjuntar imagen (usar `image_picker`)
- Preview de imagen seleccionada
- Botón "Generar Carta y Enviar" (con loading indicator)
- **BottomSheet/Diálogo** mostrando la carta formal generada con opción de "Confirmar envío" o "Editar descripción"

### Pantalla de detalle (IncidenciaDetailScreen)

- Hero animation desde la tarjeta
- SliverAppBar con imagen de evidencia (si existe) o gradiente por tipo
- Secciones con expansion tiles:
  - "Descripción del problema"
  - "Carta formal generada" (mostrar en container con tipografía monoespaciada)
  - "Evidencia fotográfica" (imagen expandible)
  - "Respuesta de administración" (visible solo si estado != PENDIENTE)
- Botón flotante (FAB) según rol:
  - Usuario normal: "Eliminar" (solo si es suya y está PENDIENTE)
  - Admin: "Responder" (abre diálogo con TextField)

---

## 6. Gestión de Imágenes (Flujo de 2 pasos)

### Paso 1: Crear la incidencia (JSON puro)

```dart
final response = await dio.post(
  '/api/v1/incidencia-service/incidencias',
  data: {
    "codigoAula": selectedAulaId,
    "descripcionBreve": descripcionController.text,
    "tipoIncidencia": selectedTipo.name, // HARDWARE, SOFTWARE, INFRAESTRUCTURA, OTRO
  },
);
final incidenciaId = response.data['incidencia']['id'];
final cartaGenerada = response.data['incidencia']['cartaFormalGenerada'];
```

### Paso 2: Subir imagen (multipart/form-data) — solo si el usuario adjuntó una

```dart
if (selectedImage != null) {
  final formData = FormData.fromMap({
    'imagen': await MultipartFile.fromFile(
      selectedImage.path,
      filename: 'evidencia_$incidenciaId.jpg',
    ),
  });

  await dio.post(
    '/api/v1/incidencia-service/incidencias/$incidenciaId/imagen',
    data: formData,
  );
}
```

### Mostrar imagen

```dart
Image.network(
  'http://incidencia-service:8085/app/uploads/incidencias/${incidencia.urlImagen}',
  headers: {'Authorization': 'Bearer $jwtToken'},
  fit: BoxFit.cover,
)
```

> **Nota:** La imagen se almacena en el servidor de archivos del contenedor Docker. La URL puede variar según configuración de nginx o proxy inverso.
>
> **IMPORTANTE**: Nunca intentes enviar la imagen dentro del `POST /incidencias` inicial. Ese endpoint solo acepta `application/json`.

---

## 7. Estados de carga y error

- **Skeleton loaders** mientras carga lista
- **Empty state** cuando no hay incidencias (ilustración + texto "No has reportado incidencias")
- **Error state** con botón de reintentar
- **Snackbar** confirmando acciones ("Incidencia creada exitosamente")
- **Pull to refresh** en listas

---

## 8. Navegación

```
/home
  └── /incidencias                    (lista de incidencias del usuario)
        ├── /incidencias/create       (formulario de creación)
        ├── /incidencias/:id          (detalle de incidencia)
        └── /incidencias/admin        (panel admin, solo si rol es ADMIN/ADMINISTRATIVO)
```

---

## 9. Integración con servicios existentes

### Obtener lista de aulas (para el selector)

```dart
// Usar el aula-service
GET http://aula-service:8083/api/v1/aula-service/aulas
```

### Verificar rol del usuario

El rol viene en el JWT. Decodificar con `jwt_decoder` o extraer del `AuthService` existente.

```dart
bool esAdmin = userRol == 'ADMINISTRADOR' || userRol == 'ADMINISTRATIVO';
```

---

## 10. Checklist de Entrega

- [ ] Pantalla de lista de incidencias con tarjetas Material 3
- [ ] Pantalla de creación con selector de aula, tipo, descripción e imagen
- [ ] Pantalla de detalle con hero animation y secciones expandibles
- [ ] Panel administrativo con lista de pendientes y contador
- [ ] Funcionalidad de responder incidencia (solo admin)
- [ ] Generación y visualización de carta formal
- [ ] Subida y visualización de imágenes
- [ ] Estados vacíos, de carga y de error
- [ ] Pull to refresh y paginación
- [ ] Snackbar de confirmación
- [ ] Navegación integrada con GoRouter o Navigator 2.0
- [ ] Pruebas con datos reales del backend

---

## 11. Ejemplo de respuesta del backend (POST /incidencias)

```json
{
  "mensaje": "La incidencia se ha reportado y guardado con éxito!",
  "incidencia": {
    "id": 1,
    "codigoAula": 123,
    "codigoUsuario": 2,
    "descripcionBreve": "El proyector no enciende y presenta manchas en la lente",
    "urlImagen": null,
    "cartaFormalGenerada": "<p>Por medio de la presente...</p>",
    "tipoIncidencia": "HARDWARE",
    "estado": "PENDIENTE",
    "respuestaAdministracion": null,
    "fechaRespuesta": null,
    "codigoAdministrador": null,
    "fechaReporte": "2026-05-25T10:30:00"
  }
}
```

---

**Implementa todo lo anterior de forma modular, reutilizable y siguiendo las mejores prácticas de Flutter (Clean Architecture o MVVM si el proyecto ya lo usa).**
