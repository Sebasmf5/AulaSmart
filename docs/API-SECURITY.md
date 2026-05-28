# API Reference: Seguridad (`security-core`)

> **Tipo:** Librería compartida (no es un servicio ejecutable)  
> **Incluida en:** `usuarios-service`, `reserva-service`, `incidencia-service`, `chat-service`  

---

## Visión General

El módulo `security-core` proporciona los mecanismos de seguridad transversales utilizados por todos los microservicios de AulaSmart. Incluye autenticación JWT, cifrado end-to-end (E2E) y configuraciones de seguridad compartidas.

---

## JWT (JSON Web Token)

### Emisor
El token es emitido y firmado por `usuarios-service`.

### Claims

| Claim | Tipo | Descripción |
|-------|------|-------------|
| `jti` | String | Código del usuario (`codigo`) |
| `sub` | String | Email del usuario |
| `nombre` | String | Nombre completo (`nombre + apellido`) |
| `rol` | String | Rol del usuario (`ADMINISTRADOR`, `ADMINISTRATIVO`, etc.) |
| `iat` | Date | Fecha de emisión |
| `exp` | Date | Fecha de expiración |

### Uso
```
Authorization: Bearer <token>
```

### Propagación entre servicios
Cuando un microservicio llama a otro (por ejemplo, `chat-service` → `reserva-service`), el token JWT se propaga automáticamente mediante un `FeignClientInterceptor`.

---

## Cifrado End-to-End (E2E)

> **Estado actual:** Activo únicamente en `usuarios-service`. Los demás servicios operan en plano.

### Algoritmos

| Capa | Algoritmo | Detalle |
|------|-----------|---------|
| Asimétrico | RSA | 1024 bits (para intercambio de clave AES) |
| Simétrico | AES-128-ECB | Con padding PKCS#5 (cifrado de payload) |

### Flujo de Handshake

```
┌─────────┐                              ┌──────────────┐
│ Cliente │                              │ Servidor     │
└────┬────┘                              └──────┬───────┘
     │                                          │
     │  GET /crypto/public-key                  │
     │─────────────────────────────────────────>│
     │                                          │
     │  { "n": "hex", "e": "hex" }              │
     │<─────────────────────────────────────────│
     │                                          │
     │  Genera AES key (16 bytes)               │
     │  Cifra AES key con RSA(n,e)              │
     │                                          │
     │  POST /crypto/handshake                  │
     │  { "encryptedAesKey": "hex" }            │
     │─────────────────────────────────────────>│
     │                                          │
     │  { "sessionId": "uuid" }                 │
     │<─────────────────────────────────────────│
     │                                          │
     │  Guarda sessionId + AES key              │
     │  Almacena en memoria (InMemorySessionStore)
```

### Formato de petición cifrada

**Headers:**
```
Content-Type: application/json
x-session-id: <session_id>
```

**Body:**
```json
{
  "payload": "<texto_cifrado_en_base64>"
}
```

### Formato de respuesta cifrada

```json
{
  "payload": "<texto_cifrado_en_base64>"
}
```

---

## Componentes Principales

### `EncryptionFilter`
Filtro Servlet que intercepta peticiones y respuestas para descifrar/cifrar automáticamente.

**Comportamiento:**
1. Si la ruta contiene `/auth/` o `/crypto/` → pasa en plano.
2. Si no hay header `x-session-id` → pasa en plano.
3. Si la sesión no existe → pasa en plano.
4. Si el `Content-Type` es `multipart/form-data` → pasa en plano (para subida de archivos).
5. En cualquier otro caso: descifra body, procesa, cifra respuesta.

### `InMemorySessionStore`
Almacén de sesiones criptográficas en memoria con TTL de 30 minutos.

> **Nota arquitectónica:** El mapa de sesiones es `static` para garantizar que todas las instancias del componente dentro de la misma JVM compartan las mismas sesiones.

### `JwtValidationFilter`
Filtro de Spring Security que valida el token JWT en cada petición protegida.

**Extracción de claims:**
- `codigo` desde `jti`
- `rol` desde claim personalizado `rol`

**Authority generada:**
```java
new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())
```

### `CryptoService`
Servicio de cifrado/descifrado AES-128-ECB con PKCS#5 padding.

### `CryptoController`
Expone los endpoints de handshake y clave pública.

---

## Rutas Públicas (Sin autenticación)

| Ruta | Descripción |
|------|-------------|
| `POST /auth/login` | Login de usuario |
| `POST /auth/refresh` | Refresh de token |
| `GET /crypto/public-key` | Obtener clave RSA pública |
| `POST /crypto/handshake` | Intercambio de clave AES |
| `OPTIONS /**` | Preflight CORS |

---

## Consideraciones de Seguridad

1. **Password hashing:** Todas las contraseñas se almacenan con BCrypt (`$2a$`). El sistema incluye un `PasswordMigration` que verifica que no existan contraseñas en texto plano al iniciar.

2. **E2E desactivado en producción parcial:** Por cuestiones de estabilidad y tiempo de desarrollo, el cifrado E2E solo está activo en `usuarios-service`. Para una implementación full-E2E, se requiere Redis como almacén de sesiones compartido.

3. **CORS:** Configurado con `allowedOrigins: *` y `allowCredentials: false` para desarrollo. En producción debe restringirse al dominio del frontend.

4. **CSRF:** Desactivado en todos los servicios (arquitectura stateless JWT).

