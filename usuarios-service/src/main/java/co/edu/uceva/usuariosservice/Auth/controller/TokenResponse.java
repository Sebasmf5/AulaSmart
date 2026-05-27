package co.edu.uceva.usuariosservice.Auth.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("user_info")
        UserInfo userInfo
) {
    public record UserInfo(
            Long codigo,
            @JsonProperty("nombre_completo")
            String nombreCompleto,
            String email,
            String rol,
            @JsonProperty("ultimo_inicio_sesion")
            LocalDateTime ultimoInicioSesion
    ) {}
}
