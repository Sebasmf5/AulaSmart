# Arquitectura General

AulaSmart adopta una arquitectura de microservicios desacoplada, donde cada dominio de negocio opera como una aplicación Spring Boot independiente. Los servicios se comunican entre sí mediante REST y, en algunos casos, a través de Feign Clients cuando se requiere orquestación interna.

---

## Diagrama de Microservicios

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Cliente Flutter                              │
│                    (Android / iOS / Web)                             │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ HTTPS / HTTP
                           │
┌──────────────────────────▼──────────────────────────────────────────┐
│                     API Gateway / ALB (opcional)                     │
│                       Puerto 80 / 443                                │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼──────┐  ┌────────▼────────┐  ┌─────▼──────┐
│  usuarios-   │  │   reserva-      │  │  chat-     │
│  service     │  │   service       │  │  service   │
│  :8081       │  │   :8082         │  │  :8086     │
└───────┬──────┘  └────────┬────────┘  └─────┬──────┘
        │                  │                  │
        │         ┌────────▼────────┐         │
        │         │   aula-         │         │
        │         │   service       │         │
        │         │   :8083         │         │
        │         └─────────────────┘         │
        │                                     │
┌───────▼─────────────────────────────────────▼──────┐
│            incidencia-service :8085                │
└────────────────────────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
     ┌────────▼─────────┐    ┌─────────▼────────┐
     │   PostgreSQL     │    │   security-core  │
     │   (RDS / Local)  │    │   (librería)     │
     │                  │    │   JWT + E2E      │
     └──────────────────┘    └──────────────────┘
```

---

## Descripción de Microservicios

### 1. `usuarios-service` (Puerto 8081)
Gestiona el ciclo de vida de los usuarios del sistema: autenticación, autorización, creación, modificación y eliminación de cuentas. Es el único servicio que mantiene activo el cifrado punto a punto (E2E) en sus rutas de negocio.

**Responsabilidades clave:**
- Autenticación mediante JWT (access + refresh tokens).
- Control de acceso basado en roles (`ADMINISTRADOR`, `ADMINISTRATIVO`, `DOCENTE`, `ESTUDIANTE`).
- Intercambio de claves criptográficas (RSA handshake) para sesiones E2E.
- Reglas de negocio de creación de usuarios por rol.

### 2. `aula-service` (Puerto 8083)
Catálogo maestro de espacios físicos de la universidad. Expone metadatos sobre aulas, bloques, facultades y tipos de espacio.

**Responsabilidades clave:**
- CRUD de aulas, bloques, facultades y tipos de aula.
- Integración con el sistema externo **SIGA** para sincronización de espacios.
- Verificación de requisitos de autorización por tipo de aula.

### 3. `reserva-service` (Puerto 8082)
Orquesta la solicitud, aprobación y gestión de reservas de aulas. Opera en texto plano (E2E desactivado para este servicio).

**Responsabilidades clave:**
- Creación de reservas con validación de solapamiento (interno + SIGA).
- Restricciones por rol de solicitante (ej: estudiantes solo aulas interactivas tipo 78/79).
- Aprobación o rechazo de reservas pendientes por personal administrativo.
- Optimistic locking para evitar condiciones de carrera en modificaciones.

### 4. `chat-service` (Puerto 8086)
Asistente conversacional basado en inteligencia artificial que permite a los usuarios realizar consultas y acciones mediante lenguaje natural.

**Responsabilidades clave:**
- Procesamiento de mensajes mediante LLM (`kimi-k2.6` vía OpenCode Go).
- Integración con herramientas: reservar aula, consultar disponibilidad, contar aulas.
- Memoria de conversación con TTL (Caffeine cache).
- Ejecución asíncrona de herramientas con propagación de contexto de seguridad.

### 5. `incidencia-service` (Puerto 8085)
Gestiona el reporte y seguimiento de incidencias (daños, fallas, sugerencias) en las aulas. Incluye generación automática de cartas formales mediante IA.

**Responsabilidades clave:**
- Reporte de incidencias con generación de carta formal (IA).
- Subida y consulta de imágenes de evidencia.
- Panel administrativo para respuesta y cambio de estado.
- Estados: `PENDIENTE` → `REVISADA` → `CERRADA`.

### 6. `security-core` (Librería compartida)
No es un servicio ejecutable, sino una librería Maven incluida en los demás microservicios.

**Responsabilidades clave:**
- Filtro de cifrado E2E (`EncryptionFilter`) con soporte AES-ECB / RSA.
- Almacén de sesiones criptográficas (`InMemorySessionStore`).
- Filtro de validación JWT (`JwtValidationFilter`).
- Configuraciones de seguridad compartidas (`CommonSecurityConfig`).

---

## Base de Datos

Cada microservicio posee su propia base de datos lógica dentro de una instancia PostgreSQL.

| Servicio | Base de datos | DDL |
|----------|---------------|-----|
| `usuarios-service` | `usuariosdb` | `ddl-auto: validate` |
| `aula-service` | `auladb` | `ddl-auto: update` |
| `reserva-service` | `reservadb` | `ddl-auto: update` |
| `incidencia-service` | `incidenciadb` | `ddl-auto: update` |

> Nota: `chat-service` no requiere base de datos persistente; utiliza Caffeine para memoria de conversación en memoria.

---

## Seguridad y Comunicación

### JWT (JSON Web Token)
- **Issuer:** `usuarios-service`
- **Claims:** `jti` (codigo usuario), `sub` (email), `nombre`, `rol`
- **Expiración:** Configurable vía `application.yml`
- **Propagación:** El token se propaga entre microservicios mediante Feign interceptors.

### Cifrado E2E (End-to-End)
El cifrado E2E está activo únicamente en `usuarios-service`. Los demás servicios (`reserva-service`, `incidencia-service`, `chat-service`) operan en texto plano para simplificar la integración con el frontend.

**Flujo E2E:**
1. Cliente solicita clave pública RSA (`GET /crypto/public-key`).
2. Cliente genera clave AES de 128 bits, la cifra con RSA y envía `POST /crypto/handshake`.
3. Servidor devuelve `sessionId` y almacena la clave AES.
4. Cliente envía `x-session-id` + body cifrado (`{"payload":"..."}`) en cada petición.
5. Servidor descifra body, procesa, y cifra la respuesta en el mismo formato.

---

## Puertos y URLs Base

| Servicio | Puerto Host (Docker) | URL Base |
|----------|---------------------|----------|
| `usuarios-service` | `8081` | `/api/v1` |
| `reserva-service` | `8082` | `/api/v1/reserva-service` |
| `aula-service` | `8083` | `/api/v1/aula-service` |
| `incidencia-service` | `8085` | `/api/v1/incidencia-service` |
| `chat-service` | `8086` | `/api/v1/chat` |

