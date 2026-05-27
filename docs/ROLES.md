# Matriz de Roles y Permisos

AulaSmart implementa un modelo de control de acceso basado en roles (RBAC) con cuatro perfiles principales. A continuación se detalla qué acciones puede realizar cada rol en cada microservicio.

---

## Perfiles de Usuario

| Rol | Descripción | Creación permitida por |
|-----|-------------|------------------------|
| **ADMINISTRADOR** | Control total del sistema. Puede gestionar usuarios, aprobar reservas, responder incidencias y acceder a todos los paneles administrativos. | Solo otro `ADMINISTRADOR` |
| **ADMINISTRATIVO** | Gestión operativa. Puede crear docentes y estudiantes, confirmar/rechazar reservas, responder incidencias, pero no crear otros administrativos ni administradores. | `ADMINISTRADOR` o `ADMINISTRATIVO` (solo DOCENTE/ESTUDIANTE) |
| **DOCENTE** | Usuario académico. Puede reservar aulas, reportar incidencias, consultar información. | `ADMINISTRADOR` o `ADMINISTRATIVO` |
| **ESTUDIANTE** | Usuario estudiantil. Puede reservar aulas interactivas (tipos 78/79), reportar incidencias, consultar información. | `ADMINISTRADOR` o `ADMINISTRATIVO` |

---

## Permisos por Microservicio

### `usuarios-service`

| Endpoint | ADMINISTRADOR | ADMINISTRATIVO | DOCENTE | ESTUDIANTE |
|----------|:-------------:|:--------------:|:-------:|:----------:|
| `POST /auth/login` | ✅ | ✅ | ✅ | ✅ |
| `POST /auth/refresh` | ✅ | ✅ | ✅ | ✅ |
| `GET /crypto/public-key` | ✅ | ✅ | ✅ | ✅ |
| `POST /crypto/handshake` | ✅ | ✅ | ✅ | ✅ |
| `GET /usuario-service/usuarios` | ✅ | ✅ | ❌ | ❌ |
| `GET /usuario-service/usuarios/page/{page}` | ✅ | ✅ | ❌ | ❌ |
| `POST /usuario-service/usuarios` | ✅ | ✅* | ❌ | ❌ |
| `PUT /usuario-service/usuarios/{id}` | ✅ | ✅* | ❌ | ❌ |
| `DELETE /usuario-service/usuarios/{id}` | ✅ | ✅ | ❌ | ❌ |
| `GET /usuario-service/usuarios/{id}` | ✅ | ✅ | ✅ | ✅ |

> \* El rol `ADMINISTRATIVO` solo puede crear/actualizar usuarios con rol `DOCENTE` o `ESTUDIANTE`. Si intenta crear un `ADMINISTRADOR`, `ADMINISTRATIVO` o `Monitor`, el sistema responde con HTTP 403.

---

### `aula-service`

| Endpoint | ADMINISTRADOR | ADMINISTRATIVO | DOCENTE | ESTUDIANTE |
|----------|:-------------:|:--------------:|:-------:|:----------:|
| `GET /aula-service/aulas` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/codigo/{codigo}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/tipo/{codigo}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/siga/{codigo}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/requiere-autorizacion/{codigo}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/bloque/{bloqueId}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/facultad/{facultadId}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/buscar/{nombreAula}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/tipo-aula/{tipoAula}` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/codigos` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/codigos-siga` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/aulas/sincronizadas-siga` | ✅ | ✅ | ✅ | ✅ |
| `POST /aula-service/aulas` | ✅ | ✅ | ❌ | ❌ |
| `PUT /aula-service/aulas` | ✅ | ✅ | ❌ | ❌ |
| `DELETE /aula-service/aulas/{id}` | ✅ | ✅ | ❌ | ❌ |
| `GET /aula-service/tipos-aula` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/facultades` | ✅ | ✅ | ✅ | ✅ |
| `GET /aula-service/bloques` | ✅ | ✅ | ✅ | ✅ |

---

### `reserva-service`

| Endpoint | ADMINISTRADOR | ADMINISTRATIVO | DOCENTE | ESTUDIANTE |
|----------|:-------------:|:--------------:|:-------:|:----------:|
| `GET /reserva-service/reservas` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/page/{page}` | ✅ | ✅ | ✅ | ✅ |
| `POST /reserva-service/reservas` | ✅ | ✅ | ✅ | ✅ |
| `DELETE /reserva-service/reservas/{id}` | ✅ | ✅ | ✅ | ✅ |
| `PUT /reserva-service/reservas` | ✅ | ✅ | ❌ | ❌ |
| `GET /reserva-service/reservas/mis-reservas` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/usuario/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/aula/{aulaId}/agregadas` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/ocupadas` | ✅ | ✅ | ✅ | ✅ |
| `GET /reserva-service/reservas/pendientes` | ✅ | ❌ | ❌ | ❌ |
| `PUT /reserva-service/reservas/{id}/confirmar` | ✅ | ❌ | ❌ | ❌ |
| `PUT /reserva-service/reservas/{id}/rechazar` | ✅ | ❌ | ❌ | ❌ |

> **Nota de negocio:** Los estudiantes solo pueden reservar aulas de tipo `78` o `79` (interactivas/audiovisuales). Si intentan reservar otro tipo, el sistema responde con HTTP 409.

---

### `incidencia-service`

| Endpoint | ADMINISTRADOR | ADMINISTRATIVO | DOCENTE | ESTUDIANTE |
|----------|:-------------:|:--------------:|:-------:|:----------:|
| `GET /incidencia-service/incidencias` | ✅ | ✅ | ✅ | ✅ |
| `GET /incidencia-service/incidencias/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /incidencia-service/incidencias/page/{page}` | ✅ | ✅ | ✅ | ✅ |
| `POST /incidencia-service/incidencias` | ✅ | ✅ | ✅ | ✅ |
| `POST /incidencia-service/incidencias/{id}/imagen` | ✅ | ✅ | ✅ | ✅ |
| `GET /incidencia-service/incidencias/{id}/imagen` | ✅ | ✅ | ✅ | ✅ |
| `PUT /incidencia-service/incidencias/{id}` | ✅ | ✅ | ❌ | ❌ |
| `DELETE /incidencia-service/incidencias/{id}` | ✅ | ❌ | ❌ | ❌ |
| `GET /incidencia-service/incidencias/pendientes` | ✅ | ✅ | ❌ | ❌ |
| `GET /incidencia-service/incidencias/pendientes/count` | ✅ | ✅ | ❌ | ❌ |
| `PUT /incidencia-service/incidencias/{id}/responder` | ✅ | ✅ | ❌ | ❌ |

---

### `chat-service`

| Endpoint | ADMINISTRADOR | ADMINISTRATIVO | DOCENTE | ESTUDIANTE |
|----------|:-------------:|:--------------:|:-------:|:----------:|
| `POST /chat` | ✅ | ✅ | ✅ | ✅ |
| `POST /chat/reset` | ✅ | ✅ | ✅ | ✅ |

> El chat-service no restringe por rol; cualquier usuario autenticado puede utilizar el asistente virtual.

---

## Jerarquía Visual de Permisos

```
ADMINISTRADOR
├── Crear/Editar/Eliminar: ADMINISTRADOR, ADMINISTRATIVO, DOCENTE, ESTUDIANTE
├── Confirmar/Rechazar reservas
├── Responder incidencias
└── Acceso total a paneles

ADMINISTRATIVO
├── Crear/Editar: DOCENTE, ESTUDIANTE (NO administradores)
├── Responder incidencias
└── Ver reservas pendientes

DOCENTE
├── Reservar aulas (cualquier tipo)
├── Reportar incidencias
└── Consultar catálogo

ESTUDIANTE
├── Reservar aulas interactivas (tipo 78/79)
├── Reportar incidencias
└── Consultar catálogo
```

