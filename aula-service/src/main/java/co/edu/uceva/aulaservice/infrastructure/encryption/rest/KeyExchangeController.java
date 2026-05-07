package co.edu.uceva.aulaservice.infrastructure.encryption.rest;

import co.edu.uceva.security.models.KeyExchangeDto;
import co.edu.uceva.security.protocol.KeyExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint público de intercambio de llave AES para aula-service.
 * Accesible sin JWT ({@code /api/v1/crypto/**} es {@code permitAll}).
 */
@RestController
@RequestMapping("/api/v1/crypto")
public class KeyExchangeController {

    private final KeyExchangeService keyExchangeService;

    public KeyExchangeController(KeyExchangeService keyExchangeService) {
        this.keyExchangeService = keyExchangeService;
    }

    @PostMapping("/key-exchange")
    public ResponseEntity<Map<String, String>> keyExchange(@RequestBody KeyExchangeDto dto) {
        String sessionId = keyExchangeService.processKeyExchange(dto);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}
