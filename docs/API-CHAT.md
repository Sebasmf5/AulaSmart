# API Reference: `chat-service`

> **Puerto:** `8086`  
> **URL Base:** `http://<host>:8086/api/v1/chat`  
> **E2E:** Desactivado. Peticiones en plano.  

---

## Chatbot IA

### `POST /chat`
Envía un mensaje al asistente virtual y recibe una respuesta generada por inteligencia artificial.

**Acceso:** Todos los usuarios autenticados.

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <jwt_token>
```

**Request Body:**
```json
{
  "message": "Quiero reservar el aula 195 mañana a las 8am"
}
```

**Response 200:**
```json
{
  "respuesta": "He creado tu reserva para el aula 195 el 27 de mayo de 2026 de 08:00 a 10:00. Estado: CONFIRMADA.",
  "accion": "RESERVA_CREADA",
  "datos": {
    "idReserva": 15,
    "aulaId": 195,
    "horaInicio": "2026-05-27T08:00:00",
    "horaFin": "2026-05-27T10:00:00"
  }
}
```

**Capacidades del asistente:**
- Responder preguntas generales sobre aulas, horarios y disponibilidad.
- Crear reservas mediante herramientas (`reservarAulaTool`).
- Consultar aulas ocupadas en rangos de tiempo.
- Contar aulas disponibles.

**Tecnología:**
- Modelo: `kimi-k2.6` vía OpenCode Go API (`https://opencode.ai/zen/go/v1`)
- Framework: Spring AI con `OpenAiSdkChatModel`
- Memoria: Caffeine Cache con TTL por usuario

---

### `POST /chat/reset`
Limpia el historial de conversación y la memoria caché del usuario autenticado.

**Acceso:** Todos los usuarios autenticados.

**Response 200:**
```json
{
  "mensaje": "Conversación reiniciada exitosamente."
}
```

---

## Arquitectura Interna

### Propagación de Contexto de Seguridad

El `chat-service` ejecuta llamadas al LLM de forma asíncrona. Para mantener el JWT disponible dentro de los hilos asíncronos, se utiliza:

1. `SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)` en la clase principal.
2. Captura del JWT antes de lanzar `CompletableFuture`:
   ```java
   String jwt = AuthContext.getJwt();
   ```
3. Restauración dentro del hilo asíncrono:
   ```java
   AuthContext.setJwt(jwt);
   ```

### Herramientas (Tools)

El asistente dispone de las siguientes funciones:

| Tool | Descripción | Servicio destino |
|------|-------------|------------------|
| `consultarAulaTool` | Obtiene información de un aula por código | `aula-service` |
| `consultarAulasOcupadasTool` | Lista aulas ocupadas en un rango horario | `reserva-service` |
| `reservarAulaTool` | Crea una reserva para el usuario autenticado | `reserva-service` |
| `contarAulasTool` | Cuenta el total de aulas registradas | `aula-service` |

---

## Modelo de Datos

### `ChatRequest`

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `message` | String | ✅ | Mensaje del usuario (máx. 500 caracteres) |

