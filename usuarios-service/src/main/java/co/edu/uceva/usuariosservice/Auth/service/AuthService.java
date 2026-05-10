package co.edu.uceva.usuariosservice.Auth.service;

import co.edu.uceva.usuariosservice.Auth.controller.AuthRequest;
import co.edu.uceva.usuariosservice.Auth.controller.LoginRequest;
import co.edu.uceva.usuariosservice.Auth.controller.TokenResponse;
import co.edu.uceva.usuariosservice.domain.excepcions.UsuarioNoEncontradoException;
import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final IUsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(IUsuarioRepository usuarioRepository,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.codigo(),
                        request.password()
                )
        );
        Usuario usuario = usuarioRepository.findById(request.codigo())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.codigo()));

        String jwtToken = jwtService.generateToken(usuario);
        String jwtRefreshToken = jwtService.generateRefreshToken(usuario);

        usuario.setUltimoInicioSesion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new TokenResponse(
                jwtToken,
                jwtRefreshToken,
                "Bearer",
                jwtService.getJwtExpiration(),
                new TokenResponse.UserInfo(
                        usuario.getCodigo(),
                        usuario.getNombre() + " " + usuario.getApellido(),
                        usuario.getEmail(),
                        usuario.getRol(),
                        usuario.getUltimoInicioSesion()
                )
        );
    }

    public TokenResponse refreshToken(final String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Bearer token");
        }

        final String refreshToken = authHeader.substring(7);
        final Long usuarioCodigo = jwtService.extractCodigo(refreshToken);

        if (usuarioCodigo == null) {
            throw new IllegalArgumentException("Invalid Refresh Token");
        }

        Usuario usuario = usuarioRepository.findById(usuarioCodigo)
                .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioCodigo));

        if (!jwtService.isTokenValid(refreshToken, usuario)) {
            throw new RuntimeException("Token no válido");
        }

        final String accessToken = jwtService.generateToken(usuario);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getJwtExpiration(),
                new TokenResponse.UserInfo(
                        usuario.getCodigo(),
                        usuario.getNombre() + " " + usuario.getApellido(),
                        usuario.getEmail(),
                        usuario.getRol(),
                        usuario.getUltimoInicioSesion()
                )
        );
    }

    public TokenResponse authenticate(final AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.id(),
                        request.password()
                )
        );
        final Usuario usuario = usuarioRepository.findById(request.id())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.id()));

        final String accessToken = jwtService.generateToken(usuario);
        final String refreshToken = jwtService.generateRefreshToken(usuario);

        usuario.setUltimoInicioSesion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getJwtExpiration(),
                new TokenResponse.UserInfo(
                        usuario.getCodigo(),
                        usuario.getNombre() + " " + usuario.getApellido(),
                        usuario.getEmail(),
                        usuario.getRol(),
                        usuario.getUltimoInicioSesion()
                )
        );
    }
}
