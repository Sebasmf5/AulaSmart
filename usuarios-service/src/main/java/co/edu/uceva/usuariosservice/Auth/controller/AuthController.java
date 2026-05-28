package co.edu.uceva.usuariosservice.Auth.controller;

import co.edu.uceva.usuariosservice.Auth.service.AuthService;
import co.edu.uceva.usuariosservice.delivery.rest.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> authenticate(@RequestBody final LoginRequest request){
        try {
            final TokenResponse token = service.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login exitoso", token, HttpStatus.OK.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Error de autenticación", e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) final String authHeader) {
        try {
            final TokenResponse token = service.refreshToken(authHeader);
            return ResponseEntity.ok(ApiResponse.success("Token refrescado exitosamente", token, HttpStatus.OK.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Error al refrescar token", e.getMessage(), HttpStatus.UNAUTHORIZED.value()));
        }
    }
}
