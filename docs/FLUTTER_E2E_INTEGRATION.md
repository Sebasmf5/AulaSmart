# Guía de Integración E2E — Frontend Flutter (AulaSmart)

> **Propósito:** Proveer a un agente de desarrollo Flutter el contexto técnico completo para integrar el cifrado extremo a extremo (E2E) con todos los microservicios de AulaSmart.

---

## 1. Arquitectura E2E (Resumen para Frontend)

El sistema usa **RSA + AES** de capa de aplicación:

| Fase | Algoritmo | Propósito |
|------|-----------|-----------|
| **Handshake** | RSA-1024 (e=65537) | Intercambiar la clave AES de forma segura |
| **Comunicación** | AES-128-ECB + PKCS7 | Cifrar el body de cada mensaje HTTP |

**Flujo visual:**
```
Flutter App                                    Backend (Spring Boot)
    │                                                │
    │  1. GET /api/v1/crypto/public-key              │
    │───────────────────────────────────────────────>│
    │  {"n": "4d81da09...", "e": "10001"}            │
    │<───────────────────────────────────────────────│
    │                                                │
    │  2. Genera clave AES aleatoria (16 bytes)      │
    │  3. Cifra AES con RSA: cipher = aesKey^e mod n │
    │                                                │
    │  4. POST /api/v1/crypto/handshake              │
    │     {"encryptedAesKey": "8f3a21bc..."}          │
    │───────────────────────────────────────────────>│
    │  {"sessionId": "a1b2c3d4-..."}                 │
    │<───────────────────────────────────────────────│
    │                                                │
    │  5. Guarda sessionId + aesKey en memoria segura│
    │                                                │
    │  6. Requests siguientes:                       │
    │     Header: x-session-id: a1b2c3d4-...        │
    │     Body:   {"payload": "AES_CIFRADO"}          │
    │───────────────────────────────────────────────>│
```

**Regla de oro:** Si el request lleva header `x-session-id`, el body DEBE ir cifrado. Si no lleva el header, el backend lo procesa en plano.

---

## 2. Microservicios y Endpoints Protegidos

Todos estos servicios tienen E2E activo en el backend:

| Servicio | Puerto | Base URL | Endpoints sensibles |
|----------|--------|----------|---------------------|
| `usuarios-service` | 8081 | `http://usuarios-service:8081` | `/api/v1/usuario-service/*`, `/api/v1/auth/*` |
| `aula-service` | 8083 | `http://aula-service:8083` | `/api/v1/aula-service/*` |
| `chat-service` | 8086 | `http://chat-service:8086` | `/api/v1/chat/*` |
| `reserva-service` | 8082 | `http://reserva-service:8082` | `/api/v1/reserva-service/*` |
| `incidencia-service` | 8085 | `http://incidencia-service:8085` | `/api/v1/incidencia-service/*` |

**Endpoints PÚBLICOS (sin E2E, sin JWT):**
- `GET /api/v1/crypto/public-key` — Obtener clave pública RSA
- `POST /api/v1/crypto/handshake` — Crear sesión criptográfica

**Nota:** El handshake puede ocurrir antes del login. No requiere autenticación.

---

## 3. Configuración de URLs en Flutter

### 3.1 `lib/core/constants/api_urls.dart`

```dart
class ApiUrls {
  static const String baseUrlUsuarios = 'http://usuarios-service:8081';
  static const String baseUrlAula = 'http://aula-service:8083';
  static const String baseUrlChat = 'http://chat-service:8086';
  static const String baseUrlReserva = 'http://reserva-service:8082';
  static const String baseUrlIncidencia = 'http://incidencia-service:8085';
}
```

> **Nota:** En desarrollo local, reemplazar los hostnames por `localhost` y los puertos mapeados (ej: `http://localhost:8086` para chat).

### 3.2 Actualizar `isSensitivePath` en `CryptoInterceptor`

```dart
static bool isSensitivePath(String path) {
  return path.contains('/aula-service/') ||
         path.contains('/usuario-service/') ||
         path.contains('/auth/') ||
         path.contains('/chat/') ||           // <-- AGREGAR
         path.contains('/reserva-service/') || // <-- AGREGAR
         path.contains('/incidencia-service/'); // <-- AGREGAR
}
```

---

## 4. Implementación del CryptoInterceptor

### 4.1 Estructura del interceptor

```dart
import 'package:dio/dio.dart';

class CryptoInterceptor extends Interceptor {
  final SessionManager _sessionManager;
  final RsaHelper _rsaHelper;
  final AesHelper _aesHelper;

  CryptoInterceptor(this._sessionManager, this._rsaHelper, this._aesHelper);

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!isSensitivePath(options.path)) {
      handler.next(options);
      return;
    }

    final host = options.uri.host;
    var session = _sessionManager.getSession(host);

    // Si no hay sesión, hacer handshake automáticamente
    if (session == null) {
      session = await _performHandshake(host);
      _sessionManager.saveSession(host, session);
    }

    // Agregar header de sesión
    options.headers['x-session-id'] = session.sessionId;

    // Cifrar el body si no es GET/DELETE
    if (options.method != 'GET' && options.method != 'DELETE') {
      final plainBody = options.data != null ? jsonEncode(options.data) : '';
      if (plainBody.isNotEmpty) {
        final encrypted = _aesHelper.encrypt(plainBody, session.aesKey);
        options.data = {'payload': encrypted};
      }
    }

    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final path = response.requestOptions.path;
    if (!isSensitivePath(path)) {
      handler.next(response);
      return;
    }

    // Descifrar respuesta si viene con payload
    if (response.data is Map && response.data['payload'] != null) {
      final host = response.requestOptions.uri.host;
      final session = _sessionManager.getSession(host);
      
      if (session != null) {
        final encrypted = response.data['payload'] as String;
        final decrypted = _aesHelper.decrypt(encrypted, session.aesKey);
        response.data = jsonDecode(decrypted);
      }
    }

    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Si el backend responde 401 por sesión inválida, rehacer handshake
    if (err.response?.statusCode == 401 && 
        err.response?.data?['error']?.toString().contains('Sesion criptografica invalida') == true) {
      final host = err.requestOptions.uri.host;
      _sessionManager.removeSession(host);
      // El próximo request hará handshake automáticamente
    }
    handler.next(err);
  }
}
```

### 4.2 Handshake automático

```dart
Future<CryptoSession> _performHandshake(String host) async {
  // 1. Obtener clave pública RSA
  final publicKeyResponse = await _dio.get('$host/api/v1/crypto/public-key');
  final n = BigInt.parse(publicKeyResponse.data['n'] as String, radix: 16);
  final e = BigInt.parse(publicKeyResponse.data['e'] as String, radix: 16);

  // 2. Generar clave AES de 16 bytes
  final aesKey = _generateRandomBytes(16);

  // 3. Cifrar clave AES con RSA
  final encryptedAesKey = _rsaHelper.encrypt(aesKey, n, e);

  // 4. Enviar handshake
  final handshakeResponse = await _dio.post(
    '$host/api/v1/crypto/handshake',
    data: {'encryptedAesKey': encryptedAesKey},
  );

  final sessionId = handshakeResponse.data['sessionId'] as String;

  return CryptoSession(sessionId: sessionId, aesKey: aesKey);
}

String _generateRandomBytes(int length) {
  final random = Random.secure();
  return List<int>.generate(length, (_) => random.nextInt(256))
      .map((b) => b.toRadixString(16).padLeft(2, '0'))
      .join();
}
```

---

## 5. Algoritmos: Parámetros Exactos

### AES
- **Modo:** ECB (Electronic Codebook)
- **Tamaño de clave:** 128 bits (16 bytes)
- **Padding:** PKCS7
- **IV:** No aplica en ECB (se usa IV de ceros internamente)
- **Librería recomendada:** `encrypt: ^5.0.1` con `AESMode.ecb`

### RSA
- **Tamaño:** ~1024 bits (2 primos de 512 bits)
- **Exponente público:** `e = 65537` (0x10001)
- **Padding:** PKCS#1 tipo 2
- **Transporte de clave pública:** `n` y `e` en hexadecimal
- **Transporte de clave cifrada:** `encryptedAesKey` en hexadecimal

---

## 6. Formato del Payload en Red

### Request cifrado (con `x-session-id`):
```http
POST /api/v1/chat HTTP/1.1
Host: chat-service:8086
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
x-session-id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Content-Type: application/json

{"payload":"x9K2mP3vQ7wL5jR8tY1nB4aC6dE0fG2hI4kJ6mL8oN0pQ..."}
```

### Response cifrada:
```json
{"payload":"yJ3kL5mN7oP9qR1sT3uV5wX7yZ9aB1cD3eF5gH7iJ9kL1mN3oP5qR7sT9uV1wX3"}
```

### Request sin `x-session-id` (modo desarrollo/testing):
```http
POST /api/v1/chat HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{"mensaje":"hola"}
```
> El backend lo procesa en plano. Útil para Postman.

---

## 7. Manejo de Errores

### 7.1 Sesión expirada o inválida
```json
HTTP/1.1 401 Unauthorized
{"error":"Sesion criptografica invalida"}
```

**Acción:** El interceptor debe:
1. Eliminar la sesión del `SessionManager`
2. Reintentar la petición (el próximo request hará handshake automático)

### 7.2 Payload inválido
```json
HTTP/1.1 400 Bad Request
{"error":"Payload invalido: [mensaje de error]"}
```

**Acción:** Revisar que el JSON cifrado tenga el formato correcto `{"payload":"..."}`.

### 7.3 Error cifrando respuesta (backend)
```json
HTTP/1.1 500 Internal Server Error
{"error":"Error cifrando respuesta"}
```

**Acción:** Probablemente la sesión fue eliminada en el backend pero el frontend aún la tiene. Forzar re-handshake.

---

## 8. Testing sin Cifrado (Postman / Desarrollo)

El `EncryptionFilter` es **permisivo por diseño**. Si no envías `x-session-id`, el request pasa en plano.

**Para probar en Postman:**
1. NO agregues el header `x-session-id`
2. Envía el body como JSON normal: `{"mensaje":"hola"}`
3. El backend responderá en JSON normal (sin `{"payload":"..."}`)

**Esto permite:**
- Desarrollo sin configurar Flutter
- Pruebas unitarias del backend
- Debugging rápido

---

## 9. Checklist de Integración

- [ ] Agregar `chat-service`, `reserva-service`, `incidencia-service` a `isSensitivePath()`
- [ ] Verificar URLs base en `ApiUrls` para los 5 servicios
- [ ] Implementar/verificar `CryptoInterceptor.onRequest()` (cifrado antes de enviar)
- [ ] Implementar/verificar `CryptoInterceptor.onResponse()` (descifrado al recibir)
- [ ] Implementar/verificar `CryptoInterceptor.onError()` (re-handshake en 401)
- [ ] Implementar/verificar `_performHandshake()` con RSA + AES correcto
- [ ] Probar handshake exitoso con cada microservicio
- [ ] Probar petición cifrada con `x-session-id`
- [ ] Probar petición en plano SIN `x-session-id` (debe funcionar igual)
- [ ] Probar re-handshake automático cuando la sesión expira
- [ ] Guardar `sessionId` y `aesKey` en `FlutterSecureStorage` o memoria segura

---

## 10. Archivos Clave de Referencia en el Backend

| Archivo | Qué contiene |
|---------|-------------|
| `security-core/filter/EncryptionFilter.java` | Filtro E2E completo |
| `security-core/crypto/CryptoService.java` | Wrapper AES+Base64 |
| `security-core/crypto/controller/CryptoController.java` | Endpoints `/public-key` y `/handshake` |
| `security-core/session/InMemorySessionStore.java` | Almacén de sesiones con TTL 30 min |
| `security-core/rsa/RsaKeyStore.java` | Generación de par RSA |
| `aula-service/config/AulaEncryptionConfig.java` | Ejemplo de registro del filtro |
| `chat-service/config/ChatEncryptionConfig.java` | Registro del filtro para chat |
| `reserva-service/config/ReservaEncryptionConfig.java` | Registro del filtro para reservas |
| `incidencia-service/config/IncidenciaEncryptionConfig.java` | Registro del filtro para incidencias |

---

## 11. Notas Importantes

1. **El E2E es transparente para los controllers.** Ni `ChatController`, `ReservaRestController` ni `IncidenciaRestController` saben que el cifrado existe.
2. **Las llamadas entre microservicios (backend a backend) NO usan E2E.** Son internas en la red Docker.
3. **El JWT se propaga por headers, no por body.** El `Authorization: Bearer ...` nunca se cifra.
4. **El handshake es lazy.** Se ejecuta automáticamente en el primer request a un host protegido.
5. **Sesiones en memoria.** Si el backend se reinicia, las sesiones se pierden. El frontend debe detectar el 401 y rehacer el handshake.

---

*Documento generado para integración completa del frontend Flutter con el sistema E2E de AulaSmart.*
