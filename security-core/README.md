# Security-Core

Módulo de criptografía para cifrado extremo-a-extremo (E2E) y en reposo en microservicios Spring Boot.

---

## 📦 Descripción

`security-core` es una librería Java que proporciona:

- **Cifrado E2E**: Encryptación/decryptación automática de cuerpos HTTP mediante interceptores Spring.
- **Encriptación en reposo**: Cifrado transparente de campos `String` en base de datos via JPA `AttributeConverter`.
- **Gestión de sesiones cifradas**: Intercambio RSA/AES + almacenamiento temporal en Redis.

Diseñado para integrarse en arquitecturas de microservicios donde la información sensible debe viajar cifrada entre servicios y almacenarse cifrada en la base de datos.

---

## 🏗️ Arquitectura

### Componentes principales

| Componente | Responsabilidad |
|------------|-----------------|
| `RequestBodyDecryptionAdvice` | Intercepta requests, descifra `EncryptedPayloadDto` → objeto Java |
| `ResponseBodyEncryptionAdvice` | Intercepta responses, cifra objetos Java → `EncryptedPayloadDto` |
| `KeyExchangeService` | Procesa intercambio RSA/AES y genera `sessionId` |
| `SessionKeyStore` | CRUD de llaves AES en Redis (TTL 30 min) |
| `EncryptionContext` | Bean request-scoped: almacena llave AES y `sessionId` activos |
| `EncryptDatabaseConverter` | AttributeConverter JPA: cifra/descifra campos `String` en BD |
| `JwtSessionIdExtractor` | Extrae `sessionId` desde JWT claims o header HTTP |

---

## 🔐 Protocolo E2E

### Flujo de comunicación

```
┌─────────┐                             ┌──────────────┐                              ┌──────┐
│ Cliente │                             │ Microservicio│                              │Redis │
└────┬────┘                             └──────┬───────┘                              └──┬───┘
     │                                        │                                          │
     │ 1. POST /key-exchange                  │                                          │
     │    {encryptedAesKey: Base64(RSA(pub).enc(AES_key))}                     │
     │ ──────────────────────────────────────>│                                          │
     │                                        │ 2. RSA(priv).dec()                       │
     │                                        │    → AES_key (16 bytes)                  │
     │                                        │ 3. Redis.set("session:aes:{sessionId}", AES_key, 30m)
     │                                        │ ────────────────────────────────────────>│
     │                                        │<────────────────────────────────────────│
     │ 4. {sessionId: "uuid"}                 │                                          │
     │<───────────────────────────────────────│                                          │
     │                                        │                                          │
     │ 5. POST /api/...                       │                                          │
     │    Header: X-Session-ID: {sessionId}   │                                          │
     │    Body: {encryptedData: Base64(AES.enc(JSON))}                               │
     │ ──────────────────────────────────────>│                                          │
     │                                        │ 6. Redis.get("session:aes:{sessionId}")  │
     │                                        │ ────────────────────────────────────────>│
     │                                        │<────────────────────────────────────────│
     │                                        │ 7. AES.dec(ciphertext) → JSON            │
     │                                        │ 8. ObjectMapper → Objeto Java           │
     │                                        │                                          │
     │                                        │── Respuesta (Objeto) ──────────────────>│
     │                                        │                                          │
     │                                        │ 9. ObjectMapper → JSON                   │
     │                                        │10. AES.enc(json_bytes)                   │
     │                                        │11. Return {encryptedData, sessionId}    │
     │ 12. EncryptedPayloadDto                │                                          │
     │<───────────────────────────────────────│                                          │

```

### Detalle步骤

1. **Key Exchange** (sin JWT)
   - Cliente genera AES-128 aleatoria (16 bytes).
   - Cifra AES con RSA-pública (BigInt exponente `e`, módulo `n`).
   - Envía `KeyExchangeDto{encryptedAesKey}` a `POST /api/v1/crypto/key-exchange`.
   - Servidor descifra con RSA-privada (`d`, `n`), almacena AES en Redis, devuelve `sessionId`.

2. **Request Cifrado** (con JWT opcional)
   - Cliente incluye header `X-Session-ID: {sessionId}`.
   - Body: `EncryptedPayloadDto{encryptedData: Base64(AES.enc(JSON)), sessionId}` (opcional `sessionId` en body).
   - Advice lee `sessionId` (prioridad: JWT claim → header HTTP → campo DTO).
   - Recupera AES de Redis, descifra, deserializa JSON a objeto real, inyecta llave en `EncryptionContext`.

3. **Response Cifrado**
   - Controlador devuelve objeto POJO normal.
   - Advice detecta llave en `EncryptionContext`, serializa a JSON, cifra con AES, devuelve `EncryptedPayloadDto`.

---

## 🛠️ Instalación

### En el módulo security-core

```bash
cd security-core
mvn clean install
```

Esto instala `security-core-1.0-SNAPSHOT.jar` en el repositorio local Maven.

---

## 🔧 Configuración en Microservicio Consumidor

### 1. Dependencia Maven (`pom.xml`)

```xml
<dependency>
    <groupId>co.edu.uceva</groupId>
    <artifactId>security-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Escanear paquetes de seguridad

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "co.edu.uceva.tuServicio",
    "co.edu.uceva.security" // <- REQUERIDO
})
public class TuServicioApplication { }
```

### 3. Configuración de beans (`CryptoConfig.java`)

```java
@Configuration
public class CryptoConfig {

    @Value("${crypto.rsa.private-d}")
    private String rsaPrivateDHex;

    @Value("${crypto.rsa.public-n}")
    private String rsaPublicNHex;

    @Bean
    public KeyExchangeService keyExchangeService(SessionKeyStore sessionKeyStore) {
        return new KeyExchangeService(
            new BigInteger(rsaPrivateDHex, 16),
            new BigInteger(rsaPublicNHex, 16),
            sessionKeyStore
        );
    }
}
```

### 4. Configuración de seguridad (`SecurityConfig.java`)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/crypto/**").permitAll() // Key exchange público
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS));

        return http.build();
    }
}
```

### 5. Controller de key-exchange

```java
@RestController
@RequestMapping("/api/v1/crypto")
public class KeyExchangeController {

    private final KeyExchangeService keyExchangeService;

    @PostMapping("/key-exchange")
    public ResponseEntity<Map<String, String>> keyExchange(@RequestBody KeyExchangeDto dto) {
        String sessionId = keyExchangeService.processKeyExchange(dto);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}
```

### 6. application.properties

```properties
# Redis ( obligatorio )
spring.data.redis.host=localhost
spring.data.redis.port=6379

# RSA claves (hexadecimal)
crypto.rsa.private-d=${RSA_MASTER_D}
crypto.rsa.public-n=${RSA_MASTER_N}

# Encriptación BD (opcional, sólo si usas EncryptDatabaseConverter)
crypto.db.master-key=${DB_MASTER_KEY}
```

---

## 🎯 Uso en Controladores

Los controladores **no requieren cambios**. Trabajan con objetos POJO:

```java
@RestController
@RequestMapping("/api/v1/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService service;

    @PostMapping
    public ResponseEntity<AulaDto> crear(@RequestBody AulaDto dto) {
        // dto ya llega descifrado por RequestBodyDecryptionAdvice
        AulaDto guardado = service.crear(dto);
        // Se cifrará automáticamente por ResponseBodyEncryptionAdvice
        return ResponseEntity.ok(guardado);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AulaDto> obtener(@PathVariable Long id) {
        // Response se cifra automáticamente
        return ResponseEntity.ok(service.obtener(id));
    }
}
```

---

## 🔒 Encriptación en Reposo (BD)

### Marcar campos sensibles

```java
@Entity
public class Usuario {

    @Id
    private Long id;

    @Convert(converter = EncryptDatabaseConverter.class)
    @Column(name = "email")
    private String email;

    @Convert(converter = EncryptDatabaseConverter.class)
    @Column(name = "telefono")
    private String telefono;

    // getters/setters
}
```

> **Nota**: La llave maestra (`crypto.db.master-key`) debe ser **exactamente 16 bytes** (AES-128). Generar con:
> ```bash
> openssl rand -base64 16
> ```

---

## 📋 Modelos de Datos

### DTOs de transporte

**`KeyExchangeDto`** (entrada key-exchange):
```json
{
  "encryptedAesKey": "Base64(RSA(pub).enc(AES_key))"
}
```

**`EncryptedPayloadDto`** (cuerpos HTTP cifrados):
```json
{
  "encryptedData": "Base64(AES.enc(JSON_bytes))",
  "sessionId": "uuid" // opcional, se resuelve por JWT/header también
}
```

---

## 🔄 Gestión de Sesiones

- **Almacenamiento**: Redis `session:aes:{sessionId}` → AES key (Base64).
- **TTL**: 30 minutos por defecto (configurable en `SessionKeyStore`).
- **Invalidación**: Llamar `sessionKeyStore.deleteKey(sessionId)` para cerrar sesión.
- **Renovación**: Cliente puede hacer nuevo key-exchange para obtener nueva sesión.

---

## ⚠️ Consideraciones de Seguridad

### Aspectos positivos
- Separación de capas: E2E (AES por sesión) y BD (AES maestra).
- Sesiones efímeras en Redis con TTL.
- Transparencia al desarrollador (POJOS normales).

### Limitaciones conocidas
- **AES-128 ECB** (sin IV) → determinista. Patrones en texto plano detectable. Aceptable para datos de baja entropía (emails, teléfonos).
- **Clave maestra BD estática** → si se filtra, toda la BD se expone. Considerar rotación manual.
- **Dependencia de Redis**: Si Redis falla, requests con sesión activa fallan (no se puede descifrar).
- **RSA sin padding** → implementación propia sobre `BigInteger`. Asegurar que las claves sean seguras (2048+ bits).

---

## 🧪 Testing

Tests unitarios en `src/test/java`:
- `aes/TestAES.java` — validación cifrado/descifrado AES.
- `rsa/TestRSA.java` — intercambio RSA.
- `aes/tests/TestAESKeyExpansion.java` — expansión de claves.

---

## 📁 Estructura del Proyecto

```
security-core
├── pom.xml
└── src/main/java/co.edu.uceva.security
    ├── aes/                    # Implementación AES-128 (ECB)
    │   ├── AES.java
    │   ├── AESBlockCipher.java
    │   ├── AESKeyExpansion.java
    │   └── transformations/    # Rondas AES (SubBytes, ShiftRows, MixColumns, AddRoundKey)
    ├── config/
    │   ├── ObjectMapperConfig.java
    │   └── exceptions/
    │       └── CryptoException.java
    ├── converter/
    │   └── EncryptDatabaseConverter.java  # JPA AttributeConverter
    ├── models/
    │   ├── EncryptedPayloadDto.java
    │   └── KeyExchangeDto.java
    ├── protocol/
    │   ├── EncryptionContext.java   # Request-scoped bean
    │   └── KeyExchangeService.java
    ├── redis/
    │   └── SessionKeyStore.java     # Redis CRUD llaves AES
    ├── rsa/
    │   ├── RSA.java
    │   ├── RSAKeyPair.java
    │   └── utils/                   # Generación primos, Miller-Rabin
    └── spring/
        ├── RequestBodyDecryptionAdvice.java
        ├── ResponseBodyEncryptionAdvice.java
        └── JwtSessionIdExtractor.java
```

---

## 🚀 Integración en Microservicios Existentes

Los microservicios del proyecto ya integran security-core:

| Microservicio | Configuración | Controller Key-Exchange |
|---------------|---------------|------------------------|
| `aula-service` | `CryptoConfig`, `SecurityConfig` | `KeyExchangeController` en `/api/v1/crypto` |
| `reserva-service` | `CryptoConfig`, `SecurityConfig` | `KeyExchangeController` en `/api/v1/crypto` |
| `incidencia-service` | `CryptoConfig`, `SecurityConfig` | `KeyExchangeController` en `/api/v1/crypto` |
| `usuarios-service` | `CryptoConfig`, `SecurityConfig` | `KeyExchangeController` en `/api/v1/crypto` |

Todos comparten:
- Mismo par de claves RSA (hex en `application.properties`).
- Misma instancia Redis.
- DTOs comunes desde `security-core`.

---

## 🔧 Debugging

### Logs
Los `CryptoException` incluyen causa raíz. Habilitar logs:

```properties
logging.level.co.edu.uceva.security=DEBUG
```

### Errores comunes

| Error | Causa probable | Solución |
|-------|----------------|----------|
| `No se pudo determinar sessionId` | Falta header `X-Session-ID` o JWT sin claim `sessionId` | Enviar header o incluir claim en token |
| `Llave AES no tiene 16 bytes` | `encryptedAesKey` mal formado | Verificar RSA cifrado correcto |
| `Redis connection refused` | Redis no disponible | Iniciar Redis (`docker run -p 6379:6379 redis`) |
| `CryptoException en BD` | `crypto.db.master-key` inválido | Generar llave de 16 bytes Base64 válida |

---

## 📚 Referencias

- **Spring Web MVC**: `ResponseBodyAdvice`, `RequestBodyAdviceAdapter`.
- **Spring Security**: `SecurityContextHolder`, `Authentication`.
- **Spring Data Redis**: `StringRedisTemplate`.
- **Jakarta Persistence**: `AttributeConverter`.
- **Criptografía**: AES-128 ECB, RSA (modular exponentiation con `BigInteger`).

---

## 📄 Licencia

Proyecto académico UCEVA - 2025.
