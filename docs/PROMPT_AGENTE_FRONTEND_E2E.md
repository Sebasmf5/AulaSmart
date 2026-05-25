# Prompt Completo — Integración E2E Flutter (AulaSmart)

> **Enviar este documento íntegro a tu agente de IA del frontend.**
> Contiene todo el contexto, código y pasos necesarios para que el interceptor criptográfico funcione correctamente con todos los microservicios de AulaSmart.

---

## 1. Contexto del Sistema

AulaSmart es una aplicación universitaria con arquitectura de microservicios. El backend implementa **cifrado extremo a extremo (E2E)** de capa de aplicación usando **RSA + AES**.

### Microservicios con E2E activo

| Servicio | Puerto (Docker) | URLs protegidas |
|----------|----------------|-----------------|
| `usuarios-service` | 8081 | `/api/v1/usuario-service/*`, `/api/v1/auth/*` |
| `aula-service` | 8083 | `/api/v1/aula-service/*` |
| `chat-service` | 8086 | `/api/v1/chat/*` |
| `reserva-service` | 8082 | `/api/v1/reserva-service/*` |
| `incidencia-service` | 8085 | `/api/v1/incidencia-service/*` |

### Endpoints PÚBLICOS (nunca cifrar, nunca enviar `x-session-id`)

- `GET /api/v1/crypto/public-key` — Obtener clave pública RSA
- `POST /api/v1/crypto/handshake` — Crear sesión criptográfica
- `POST /api/v1/auth/login` — Iniciar sesión
- `POST /api/v1/auth/register` — Registro (si existe)

**Regla de oro:** Si la URL contiene `/auth/` o `/crypto/`, el body va en **plano** y **no se envía** `x-session-id`.

---

## 2. Arquitectura del Cifrado

### Fase 1: Handshake RSA (una vez por sesión/host)

```
Flutter App                                    Backend (Spring Boot)
    │                                                │
    │  1. GET /api/v1/crypto/public-key              │
    │───────────────────────────────────────────────>│
    │  {"n": "4d81da09...", "e": "10001"}            │
    │<───────────────────────────────────────────────│
    │                                                │
    │  2. Generar clave AES aleatoria (16 bytes)     │
    │  3. Cifrar AES con RSA: cipher = aesKey^e mod n│
    │                                                │
    │  4. POST /api/v1/crypto/handshake              │
    │     {"encryptedAesKey": "8f3a21bc..."}          │
    │───────────────────────────────────────────────>│
    │  {"sessionId": "a1b2c3d4-..."}                 │
    │<───────────────────────────────────────────────│
    │                                                │
    │  5. Guardar sessionId + aesKey en memoria segura│
```

### Fase 2: Comunicación AES (cada request/response)

**Request del frontend:**
```http
POST /api/v1/chat HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
x-session-id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Content-Type: application/json

{"payload":"x9K2mP3vQ7wL5jR8tY1nB4aC6dE0fG2hI4kJ6mL8oN0pQ..."}
```

**Response del backend:**
```json
{"payload":"yJ3kL5mN7oP9qR1sT3uV5wX7yZ9aB1cD3eF5gH7iJ9kL1mN3oP5qR7sT9uV1wX3"}
```

---

## 3. Parámetros Criptográficos Exactos

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
- **Transporte de clave pública:** `n` y `e` en hexadecimal (base 16)
- **Transporte de clave cifrada:** `encryptedAesKey` en hexadecimal

---

## 4. Configuración de URLs

### `lib/core/constants/api_urls.dart`

```dart
class ApiUrls {
  static const String baseUrlUsuarios = 'http://localhost:8081';
  static const String baseUrlAula = 'http://localhost:8083';
  static const String baseUrlChat = 'http://localhost:8086';
  static const String baseUrlReserva = 'http://localhost:8082';
  static const String baseUrlIncidencia = 'http://localhost:8085';
}
```

> En desarrollo local, usar `localhost` con los puertos mapeados. En producción, reemplazar por los hostnames Docker.

---

## 5. Implementación Completa del Interceptor

### 5.1 Modelo de sesión (`lib/models/crypto_session.dart`)

```dart
class CryptoSession {
  final String sessionId;
  final String aesKey; // Hex string de 32 caracteres (16 bytes)

  CryptoSession({required this.sessionId, required this.aesKey});
}
```

### 5.2 Gestor de sesiones (`lib/services/session_manager.dart`)

```dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/crypto_session.dart';

class SessionManager {
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  final Map<String, CryptoSession> _memorySessions = {};

  /// Obtener sesión de memoria (más rápido)
  CryptoSession? getSession(String host) {
    return _memorySessions[host];
  }

  /// Guardar sesión en memoria y persistentemente
  Future<void> saveSession(String host, CryptoSession session) async {
    _memorySessions[host] = session;
    await _storage.write(key: 'session_$host', value: '${session.sessionId}:${session.aesKey}');
  }

  /// Cargar sesión desde almacenamiento persistente
  Future<CryptoSession?> loadSession(String host) async {
    if (_memorySessions.containsKey(host)) {
      return _memorySessions[host];
    }
    
    final stored = await _storage.read(key: 'session_$host');
    if (stored != null) {
      final parts = stored.split(':');
      if (parts.length == 2) {
        final session = CryptoSession(sessionId: parts[0], aesKey: parts[1]);
        _memorySessions[host] = session;
        return session;
      }
    }
    return null;
  }

  /// Eliminar sesión (cuando expira o es inválida)
  Future<void> removeSession(String host) async {
    _memorySessions.remove(host);
    await _storage.delete(key: 'session_$host');
  }
}
```

### 5.3 Helpers criptográficos (`lib/services/crypto_helper.dart`)

```dart
import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';
import 'package:encrypt/encrypt.dart' as encrypt;

class CryptoHelper {
  /// Genera bytes aleatorios y los convierte a hex string
  static String generateRandomBytes(int length) {
    final random = Random.secure();
    return List<int>.generate(length, (_) => random.nextInt(256))
        .map((b) => b.toRadixString(16).padLeft(2, '0'))
        .join();
  }

  /// Cifra texto plano con AES-128-ECB
  static String encryptAes(String plaintext, String aesKeyHex) {
    final key = encrypt.Key(Uint8List.fromList(_hexToBytes(aesKeyHex)));
    final encrypter = encrypt.Encrypter(encrypt.AES(key, mode: encrypt.AESMode.ecb));
    final encrypted = encrypter.encrypt(plaintext);
    return encrypted.base64;
  }

  /// Descifra texto con AES-128-ECB
  static String decryptAes(String encryptedBase64, String aesKeyHex) {
    final key = encrypt.Key(Uint8List.fromList(_hexToBytes(aesKeyHex)));
    final encrypter = encrypt.Encrypter(encrypt.AES(key, mode: encrypt.AESMode.ecb));
    final encrypted = encrypt.Encrypted.fromBase64(encryptedBase64);
    return encrypter.decrypt(encrypted);
  }

  /// Cifra clave AES con RSA (cipher = aesKey^e mod n)
  static String encryptRsa(String aesKeyHex, String nHex, String eHex) {
    final n = BigInt.parse(nHex, radix: 16);
    final e = BigInt.parse(eHex, radix: 16);
    final aesKey = BigInt.parse(aesKeyHex, radix: 16);
    
    final cipher = aesKey.modPow(e, n);
    return cipher.toRadixString(16);
  }

  static List<int> _hexToBytes(String hex) {
    final bytes = <int>[];
    for (var i = 0; i < hex.length; i += 2) {
      bytes.add(int.parse(hex.substring(i, i + 2), radix: 16));
    }
    return bytes;
  }
}
```

### 5.4 Interceptor principal (`lib/interceptors/crypto_interceptor.dart`)

```dart
import 'dart:convert';
import 'package:dio/dio.dart';
import '../models/crypto_session.dart';
import '../services/crypto_helper.dart';
import '../services/session_manager.dart';

class CryptoInterceptor extends Interceptor {
  final SessionManager _sessionManager;
  final Dio _dio;

  CryptoInterceptor(this._sessionManager, this._dio);

  /// Rutas que NUNCA deben ser cifradas ni enviar x-session-id
  static bool isPublicPath(String path) {
    return path.contains('/auth/') || path.contains('/crypto/');
  }

  /// Rutas sensibles que DEBEN usar E2E
  static bool isSensitivePath(String path) {
    if (isPublicPath(path)) return false;
    return path.contains('/aula-service/') ||
           path.contains('/usuario-service/') ||
           path.contains('/chat/') ||
           path.contains('/reserva-service/') ||
           path.contains('/incidencia-service/');
  }

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final path = options.path;

    // ============================================================
    // PASO 1: Si es ruta pública, NUNCA enviar x-session-id
    // ============================================================
    if (isPublicPath(path)) {
      options.headers.remove('x-session-id');
      handler.next(options);
      return;
    }

    // ============================================================
    // PASO 2: Si no es ruta sensible, pasar normal
    // ============================================================
    if (!isSensitivePath(path)) {
      handler.next(options);
      return;
    }

    // ============================================================
    // PASO 3: Obtener o crear sesión criptográfica
    // ============================================================
    final host = options.uri.host;
    var session = await _sessionManager.loadSession(host);

    if (session == null) {
      try {
        session = await _performHandshake(host);
        await _sessionManager.saveSession(host, session);
      } catch (e) {
        return handler.reject(
          DioException(
            requestOptions: options,
            error: 'Error en handshake E2E: $e',
          ),
        );
      }
    }

    // Agregar header de sesión
    options.headers['x-session-id'] = session.sessionId;

    // ============================================================
    // PASO 4: Cifrar body si no es GET/DELETE
    // ============================================================
    if (options.method != 'GET' && options.method != 'DELETE') {
      final plainBody = options.data != null ? jsonEncode(options.data) : '';
      if (plainBody.isNotEmpty) {
        final encrypted = CryptoHelper.encryptAes(plainBody, session.aesKey);
        options.data = {'payload': encrypted};
      }
    }

    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final path = response.requestOptions.path;

    // Si es ruta pública o no sensible, pasar normal
    if (isPublicPath(path) || !isSensitivePath(path)) {
      handler.next(response);
      return;
    }

    // Descifrar respuesta si viene con payload
    if (response.data is Map && response.data['payload'] != null) {
      final host = response.requestOptions.uri.host;
      final session = _sessionManager.getSession(host);

      if (session != null) {
        try {
          final encrypted = response.data['payload'] as String;
          final decrypted = CryptoHelper.decryptAes(encrypted, session.aesKey);
          response.data = jsonDecode(decrypted);
        } catch (e) {
          // Si falla el descifrado, dejar payload original
        }
      }
    }

    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Si el backend responde 401 por sesión inválida, rehacer handshake
    if (err.response?.statusCode == 401) {
      final errorData = err.response?.data;
      final errorMsg = errorData is Map ? errorData['error']?.toString() ?? '' : '';

      if (errorMsg.contains('Sesion criptografica invalida') ||
          errorMsg.contains('sesion') ||
          errorMsg.contains('session')) {
        final host = err.requestOptions.uri.host;
        _sessionManager.removeSession(host);
        // El próximo request hará handshake automáticamente
      }
    }
    handler.next(err);
  }

  /// Handshake RSA/AES con el backend
  Future<CryptoSession> _performHandshake(String host) async {
    // 1. Obtener clave pública RSA (sin x-session-id)
    final publicKeyResponse = await _dio.get('$host/api/v1/crypto/public-key');
    final n = publicKeyResponse.data['n'] as String;
    final e = publicKeyResponse.data['e'] as String;

    // 2. Generar clave AES de 16 bytes
    final aesKey = CryptoHelper.generateRandomBytes(16);

    // 3. Cifrar clave AES con RSA
    final encryptedAesKey = CryptoHelper.encryptRsa(aesKey, n, e);

    // 4. Enviar handshake (sin x-session-id, body en plano)
    final handshakeResponse = await _dio.post(
      '$host/api/v1/crypto/handshake',
      data: {'encryptedAesKey': encryptedAesKey},
    );

    final sessionId = handshakeResponse.data['sessionId'] as String;
    return CryptoSession(sessionId: sessionId, aesKey: aesKey);
  }
}
```

### 5.5 Configuración de Dio (`lib/core/dio_client.dart`)

```dart
import 'package:dio/dio.dart';
import '../interceptors/crypto_interceptor.dart';
import '../services/session_manager.dart';

class DioClient {
  static Dio createDio(SessionManager sessionManager) {
    final dio = Dio();
    
    // Configuración base
    dio.options.connectTimeout = const Duration(seconds: 10);
    dio.options.receiveTimeout = const Duration(seconds: 30);
    
    // Agregar interceptor E2E
    dio.interceptors.add(CryptoInterceptor(sessionManager, dio));
    
    return dio;
  }
}
```

### 5.6 Servicio de autenticación (`lib/services/auth_service.dart`)

```dart
import 'package:dio/dio.dart';
import '../core/constants/api_urls.dart';
import '../services/session_manager.dart';

class AuthService {
  final Dio _dio;
  final SessionManager _sessionManager;

  AuthService(this._dio, this._sessionManager);

  Future<LoginResponse> login(String codigo, String password) async {
    // Limpiar cualquier sesión anterior para usuarios-service
    await _sessionManager.removeSession(ApiUrls.baseUrlUsuarios);

    final response = await _dio.post(
      '${ApiUrls.baseUrlUsuarios}/api/v1/auth/login',
      data: {
        'codigo': int.parse(codigo),
        'password': password,
      },
    );

    // Guardar tokens JWT
    final accessToken = response.data['data']['accessToken'];
    final refreshToken = response.data['data']['refreshToken'];
    
    // TODO: Guardar tokens en secure storage

    return LoginResponse.fromJson(response.data);
  }
}

class LoginResponse {
  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresIn;
  final UserInfo userInfo;

  LoginResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresIn,
    required this.userInfo,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    final data = json['data'];
    return LoginResponse(
      accessToken: data['accessToken'],
      refreshToken: data['refreshToken'],
      tokenType: data['tokenType'],
      expiresIn: data['expiresIn'],
      userInfo: UserInfo.fromJson(data['userInfo']),
    );
  }
}

class UserInfo {
  final int codigo;
  final String nombreCompleto;
  final String email;
  final String rol;

  UserInfo({
    required this.codigo,
    required this.nombreCompleto,
    required this.email,
    required this.rol,
  });

  factory UserInfo.fromJson(Map<String, dynamic> json) {
    return UserInfo(
      codigo: json['codigo'],
      nombreCompleto: json['nombreCompleto'],
      email: json['email'],
      rol: json['rol'],
    );
  }
}
```

---

## 6. Dependencias (`pubspec.yaml`)

```yaml
dependencies:
  flutter:
    sdk: flutter
  dio: ^5.4.0
  encrypt: ^5.0.1
  flutter_secure_storage: ^10.0.0
```

---

## 7. Flujo de Inicialización

En tu `main.dart` o `app.dart`:

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Inicializar gestor de sesiones
  final sessionManager = SessionManager();
  
  // Crear cliente Dio con interceptor E2E
  final dio = DioClient.createDio(sessionManager);
  
  // Crear servicios
  final authService = AuthService(dio, sessionManager);
  
  runApp(MyApp(dio: dio, authService: authService));
}
```

---

## 8. Manejo de Errores Específicos

### 8.1 Sesión expirada (401 con mensaje de sesión)

**Backend responde:**
```json
HTTP/1.1 401 Unauthorized
{"error":"Sesion criptografica invalida"}
```

**Acción del interceptor:**
1. Eliminar sesión del host
2. El próximo request automáticamente hará handshake nuevo
3. Reintentar la petición original (opcional, implementar con QueueInterceptor si se requiere)

### 8.2 Payload inválido (400)

**Backend responde:**
```json
HTTP/1.1 400 Bad Request
{"error":"Payload invalido: ..."}
```

**Causa probable:** Body no tiene formato `{"payload":"..."}` o está mal cifrado.

**Solución:** Revisar que el interceptor esté cifrando correctamente antes de enviar.

---

## 9. Testing en Postman (sin Flutter)

Para probar el backend sin la app Flutter, simplemente **no enviar** `x-session-id`:

```http
POST http://localhost:8081/api/v1/auth/login
Content-Type: application/json

{
  "codigo": 2,
  "password": "123456"
}
```

**Funciona en plano.** El backend detecta que no hay `x-session-id` y procesa normalmente.

---

## 10. Checklist de Integración

- [ ] Agregar dependencias: `dio`, `encrypt`, `flutter_secure_storage`
- [ ] Crear `CryptoSession` model
- [ ] Crear `SessionManager` con persistencia
- [ ] Crear `CryptoHelper` con AES + RSA
- [ ] Crear `CryptoInterceptor` con excepción para `/auth/` y `/crypto/`
- [ ] Configurar `DioClient` con el interceptor
- [ ] Modificar `AuthService.login()` para limpiar sesión antes de login
- [ ] Probar login sin `x-session-id` (debe funcionar)
- [ ] Probar petición protegida con `x-session-id` (handshake automático)
- [ ] Probar respuesta cifrada del backend (descifrado automático)
- [ ] Probar re-handshake cuando la sesión expira

---

## 11. Notas Importantes

1. **El E2E es transparente para la app.** Los controllers del backend no saben que existe.
2. **Las llamadas a `/auth/` y `/crypto/` van SIEMPRE en plano.**
3. **El JWT se envía por header `Authorization`.** Nunca se cifra.
4. **Las sesiones se pierden al reiniciar el backend.** El interceptor debe manejar re-handshake automático.
5. **Cada host tiene su propia sesión.** `usuarios-service` y `chat-service` pueden tener sesiones diferentes.

---

*Documento generado para integración completa del frontend Flutter con E2E de AulaSmart.*
