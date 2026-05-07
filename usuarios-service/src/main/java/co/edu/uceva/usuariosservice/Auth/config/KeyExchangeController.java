package co.edu.uceva.usuariosservice.Auth.config;

import co.edu.uceva.security.models.KeyExchangeDto;
import co.edu.uceva.security.protocol.KeyExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint público de intercambio de llave AES.
 *
 * <p>Accesible sin JWT ({@code /api/v1/crypto/**} es {@code permitAll} en SecurityConfig)
 * para que el cliente establezca la sesión E2E <strong>antes</strong> de autenticarse.</p>
 *
 * <h3>Flujo cliente:</h3>
 * <pre>
 * 1. POST /api/v1/crypto/key-exchange  { "encryptedAesKey": "BASE64..." }
 *    ← 200 { "sessionId": "uuid" }
 * 2. POST /api/v1/auth/login  (sin cifrar — el login siempre es claro)
 *    ← 200 { "token": "JWT con claim sessionId=uuid", ... }
 * 3. A partir de aquí, todas las peticiones cifradas llevan el JWT en Authorization.
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/crypto")
public class KeyExchangeController {

    private final KeyExchangeService keyExchangeService;

    public KeyExchangeController(KeyExchangeService keyExchangeService) {
        this.keyExchangeService = keyExchangeService;
    }

    /**
     * Recibe la llave AES cifrada con la clave pública RSA del servidor,
     * la descifra y la persiste en Redis. Devuelve el sessionId.
     *
     * @param dto {@link KeyExchangeDto} con la llave cifrada en Base64
     * @return {@code {"sessionId": "...uuid..."}}
     */
    @PostMapping("/key-exchange")
    public ResponseEntity<Map<String, String>> keyExchange(@RequestBody KeyExchangeDto dto) {
        String sessionId = keyExchangeService.processKeyExchange(dto);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}
