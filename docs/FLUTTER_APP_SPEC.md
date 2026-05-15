# Especificación Técnica: Aplicación Flutter AulaSmart

## 1. Contexto y Alcance

Esta aplicación móvil es el **cliente oficial** del ecosistema de microservicios AulaSmart. Su propósito es permitir a docentes, estudiantes y personal administrativo consultar la disponibilidad de aulas universitarias y realizar reservas de forma intuitiva.

**Restricción clave**: Esta app NO reemplaza al chatbot. Es una interfaz gráfica complementaria para usuarios que prefieren formularios y vistas estructuradas sobre conversación en lenguaje natural.

---

## 2. Arquitectura Propuesta

```
┌──────────────────────────────────────┐
│         Presentation Layer           │
│  (Screens, Widgets, BlocProviders)   │
├──────────────────────────────────────┤
│           Business Layer             │
│  (BLoC / Cubit - Estados y Eventos)  │
├──────────────────────────────────────┤
│           Domain Layer               │
│  (Use Cases, Entities, Repositories) │
├──────────────────────────────────────┤
│           Data Layer                 │
│  (API Clients, Models, DTOs, Cache)  │
└──────────────────────────────────────┘
```

**Stack recomendado**:
- **Gestión de estado**: `flutter_bloc` (BLoC pattern)
- **HTTP Client**: `dio` + `retrofit`
- **Inyección de dependencias**: `get_it` + `injectable`
- **Navegación**: `go_router`
- **Calendario/DatePicker**: `table_calendar` + `flutter_datetime_picker`
- **Almacenamiento local**: `flutter_secure_storage` (para JWT) + `hive` (cache de aulas)

---

## 3. Modelos de Datos (Consumidos del Backend)

### 3.1 Entidad `Aula`
```dart
class Aula {
  final int id;                    // PK interna (la que usamos para todo)
  final int? codigoAula;           // Código SIGA (null para aulas manuales)
  final String nombreAula;
  final int capacidad;
  final Bloque bloque;
  final TipoAula tipoAula;
  final bool sincronizadaConSiga;
}
```

### 3.2 Entidad `Bloque`
```dart
class Bloque {
  final int id;
  final String codigoEdificio;     // Ej: "B"
  final String nombre;             // Ej: "BLOQUE B - AVELLANOS"
}
```

### 3.3 Entidad `TipoAula`
```dart
class TipoAula {
  final int id;
  final String codigoTipoAula;     // Ej: "78", "79", "80"
  final String nombre;             // Ej: "AULA INTERACTIVA"
  final bool requiereAutorizacion;
}
```

### 3.4 Entidad `Reserva`
```dart
class Reserva {
  final int idReserva;
  final int aulaId;                // FK al Aula.id
  final DateTime horaInicio;
  final DateTime horaFin;
  final String estado;             // "CONFIRMADA", "PENDIENTE", "CANCELADA"
  final String? titulo;            // Motivo de la reserva
  final String? nombreUsuarioResponsable;
}
```

### 3.5 Entidad `HorarioOcupado` (para vista de disponibilidad)
```dart
class HorarioOcupado {
  final DateTime inicio;
  final DateTime fin;
  final String origen;             // "AULASMART" | "SIGA"
  final String? titulo;
}
```

---

## 4. Flujos de Pantallas (User Flows)

### 4.1 Flujo 1: Explorar Aulas y Consultar Disponibilidad

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│   Login     │────→│  Home / Dashboard │────→│   Listado de Aulas  │
└─────────────┘     └──────────────────┘     └─────────────────────┘
                                                        │
                           ┌────────────────────────────┤
                           │                            │
                    ┌──────▼──────┐            ┌────────▼────────┐
                    │ Filtro por  │            │  Detalle de Aula │
                    │ Bloque      │            │  + Calendario    │
                    │ Tipo        │            └────────┬────────┘
                    └─────────────┘                     │
                                               ┌────────▼────────┐
                                               │ Horarios del día │
                                               │ (Libres/Ocupados)│
                                               └──────────────────┘
```

**Pantallas detalladas:**

#### A. Home / Dashboard
- Tarjetas resumen: "Mis próximas reservas", "Buscar aula", "Crear reserva"
- BottomNavigationBar: Inicio | Explorar | Mis Reservas | Perfil

#### B. Listado de Aulas (`AulasScreen`)
- **AppBar** con campo de búsqueda (nombre de aula)
- **Filtros persistentes** (Chips): Bloque A, Bloque B, Laboratorio, Aula Interactiva
- **Lista vertical** con tarjetas:
  ```
  ┌─────────────────────────────┐
  │ [Icono tipo] AULA 101       │
  │ Bloque B - Avellanos        │
  │ Capacidad: 40               │
  │ [Chip] Aula Interactiva     │
  │ [Botón] Ver disponibilidad  │
  └─────────────────────────────┘
  ```
- Pull-to-refresh

#### C. Detalle de Aula (`AulaDetailScreen`)
- Header con nombre, bloque, capacidad, tipo
- **Calendario mensual** (`table_calendar`)
  - Días con reservas marcados con punto de color:
    - 🔵 AulaSmart | 🔴 SIGA | 🟣 Ambos
- Al seleccionar un día:
  - Lista de horarios ocupados con origen ("Clase SIGA: Métodos Numéricos" vs "Reserva: Reunión de facultad")
  - Bloques visuales de horarios libres (sugerencias)

---

### 4.2 Flujo 2: Crear una Reserva

```
┌─────────────────────┐
│   Detalle de Aula   │
│   (o Home Screen)   │
└──────────┬──────────┘
           │ [Botón "Reservar"]
           ▼
┌─────────────────────┐
│  Formulario Reserva │
│  ─────────────────  │
│  📅 Fecha           │ ← DatePicker
│  🕐 Hora Inicio     │ ← TimePicker
│  🕐 Hora Fin        │ ← TimePicker
│  📝 Motivo/Título   │ ← TextField (obligatorio)
│  👤 Rol visible     │ ← Solo lectura (del JWT)
│                     │
│  [Botón] Verificar  │
│         disponible  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Confirmación       │
│  ─────────────────  │
│  Aula: AULA 101     │
│  Fecha: 14/05/2026  │
│  Hora: 08:00 - 10:00│
│  Motivo: Monitorias │
│                     │
│  Estado previsto:   │
│  ✅ CONFIRMADA      │ (o ⚠️ PENDIENTE si requiere auth)
│                     │
│  [Confirmar]        │
└─────────────────────┘
```

**Validaciones del formulario (cliente)**:
- Fecha no puede ser en el pasado
- `horaFin` debe ser mayor a `horaInicio` (mínimo 30 minutos)
- Motivo no vacío (mínimo 5 caracteres)
- Si el rol es ESTUDIANTE: verificar que el tipo de aula sea 78 o 79 **antes** de enviar (UX inmediata)

**Feedback post-reserva**:
- Snackbar verde: *"¡Reserva CONFIRMADA! El aula AULA 101 ha sido reservada."*
- Snackbar naranja: *"Tu reserva quedó PENDIENTE de autorización. El administrador debe aprobarla."*
- Navegación automática a "Mis Reservas"

---

### 4.3 Flujo 3: Mis Reservas

```
┌──────────────────────────┐
│      Mis Reservas        │
├──────────────────────────┤
│ [Tabs]                   │
│ Activas | Pasadas        │
│                          │
│ ┌──────────────────────┐ │
│ │ 🏫 AULA 101          │ │
│ │ 📅 14/05/2026        │ │
│ │ 🕐 08:00 - 10:00     │ │
│ │ 📝 Monitorias        │ │
│ │ [Chip] CONFIRMADA ✅ │ │
│ │ [Cancelar]           │ │
│ └──────────────────────┘ │
└──────────────────────────┘
```

**Funcionalidades**:
- Pull-to-refresh
- Cancelar reserva (cambia estado a `CANCELADA`, no elimina)
- Filtro por estado (CONFIRMADA, PENDIENTE)

---

## 5. Servicios API (Dio Clients)

### 5.1 `AulaApiClient`
```dart
@GET("/api/v1/aula-service/aulas")
Future<List<Aula>> getAulas();

@GET("/api/v1/aula-service/aulas/bloque/{bloqueId}")
Future<List<Aula>> getAulasByBloque(@Path("bloqueId") int bloqueId);

@GET("/api/v1/aula-service/aulas/tipo-aula/{tipo}")
Future<List<Aula>> getAulasByTipo(@Path("tipo") String tipo);

@GET("/api/v1/aula-service/aulas/buscar/{nombre}")
Future<List<Aula>> buscarAulas(@Path("nombre") String nombre);

@GET("/api/v1/aula-service/bloques")
Future<List<Bloque>> getBloques();

@GET("/api/v1/aula-service/tipos-aula")
Future<List<TipoAula>> getTiposAula();
```

### 5.2 `ReservaApiClient`
```dart
@GET("/api/v1/reserva-service/reservas/ocupadas")
Future<List<int>> getAulasOcupadas(
  @Query("fecha") String fecha,
  @Query("horaInicio") String horaInicio,
  @Query("horaFin") String horaFin,
);

@GET("/api/v1/reserva-service/reservas/aula/{aulaId}/agregadas")
Future<ReservasAgregadasResponse> getReservasPorAula(@Path("aulaId") int aulaId);

@POST("/api/v1/reserva-service/reservas")
Future<Reserva> crearReserva(@Body() CrearReservaRequest request);

@GET("/api/v1/reserva-service/reservas")
Future<List<Reserva>> getMisReservas();
```

### 5.3 `AuthInterceptor` (Dio)
```dart
class AuthInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _secureStorage.read('jwt_token');
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }
}
```

---

## 6. Estados BLoC (Ejemplo: ReservaCubit)

```dart
abstract class ReservaState {}

class ReservaInitial extends ReservaState {}

class ReservaVerificando extends ReservaState {}

class ReservaDisponible extends ReservaState {
  final Aula aula;
  final DateTime fecha;
  final TimeOfDay horaInicio;
  final TimeOfDay horaFin;
  final String motivo;
}

class ReservaNoDisponible extends ReservaState {
  final String mensaje; // "El aula está ocupada de 08:00 a 10:00"
  final List<HorarioOcupado> horariosOcupados;
}

class ReservaCreada extends ReservaState {
  final Reserva reserva;
  final bool requiereAutorizacion;
}

class ReservaError extends ReservaState {
  final String mensaje; // Mensaje amigable, no stacktrace
}
```

---

## 7. Manejo de Errores (UX)

| Error Técnico | Mensaje al Usuario |
|--------------|-------------------|
| 403 JWT inválido | *"Tu sesión ha expirado. Por favor inicia sesión de nuevo."* → Navegar a Login |
| 400 Fecha en el pasado | *"No puedes reservar en fechas pasadas."* |
| 409 Solapamiento (GIST) | *"El aula ya está ocupada en ese horario. ¿Quieres ver horarios disponibles?"* |
| 500 Error interno | *"Lo sentimos, hubo un problema. Intenta de nuevo en unos momentos."* |
| Timeout / Sin red | *"Parece que no hay conexión. Verifica tu internet e intenta de nuevo."* + Retry button |

---

## 8. Consideraciones de Seguridad

1. **JWT Storage**: Usar `flutter_secure_storage` (Keychain/Keystore), NUNCA SharedPreferences
2. **Refresh Token**: Implementar interceptor de Dio que detecte 401, refresque token y reintente request
3. **Rol en UI**: El rol del usuario debe venir del JWT decodificado, NO del backend en cada pantalla (cache local)
4. **Validaciones cliente**: UX inmediata, pero NUNCA confiar solo en ellas (el backend siempre valida)

---

## 9. Estructura de Carpetas Sugerida

```
lib/
├── main.dart
├── app.dart
├── config/
│   ├── router.dart              # go_router
│   ├── theme.dart               # Material 3 theme
│   └── constants.dart           # URLs, timeouts
├── core/
│   ├── errors/
│   ├── usecases/
│   └── utils/
├── features/
│   ├── auth/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   ├── aulas/
│   │   ├── data/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   └── datasources/
│   │   ├── domain/
│   │   │   ├── entities/
│   │   │   └── repositories/
│   │   └── presentation/
│   │       ├── bloc/
│   │       ├── screens/
│   │       └── widgets/
│   ├── reservas/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   └── home/
├── shared/
│   ├── widgets/                 # LoadingIndicator, ErrorWidget, etc.
│   └── services/                # Dio client, SecureStorage
└── injection.dart               # get_it setup
```

---

## 10. Endpoints del Backend (Resumen Rápido)

| Servicio | Endpoint | Uso en Flutter |
|----------|----------|----------------|
| `aula-service` | `GET /bloques` | Listar filtros de bloques |
| `aula-service` | `GET /tipos-aula` | Listar filtros de tipos |
| `aula-service` | `GET /aulas` | Listado general |
| `aula-service` | `GET /aulas/bloque/{id}` | Filtrar por bloque |
| `aula-service` | `GET /aulas/tipo-aula/{tipo}` | Filtrar por tipo |
| `aula-service` | `GET /aulas/buscar/{nombre}` | Búsqueda fuzzy |
| `reserva-service` | `GET /reservas/ocupadas` | Verificar disponibilidad |
| `reserva-service` | `GET /reservas/aula/{id}/agregadas` | Horarios del aula (BD + SIGA) |
| `reserva-service` | `POST /reservas` | Crear reserva |

---

## 11. Próximos Pasos Sugeridos

1. **Generar API clients** con `retrofit` + `dio` usando los DTOs del backend
2. **Crear el tema visual** (colores institucionales de UCEVA)
3. **Implementar Login** (consumir `usuarios-service`)
4. **Construir `AulasScreen`** con filtros y búsqueda
5. **Construir `AulaDetailScreen`** con calendario
6. **Construir `ReservaFormScreen`**
7. **Integrar notificaciones push** para recordatorios de reservas

---

*Documento generado para el proyecto AulaSmart - Frontend Flutter*
