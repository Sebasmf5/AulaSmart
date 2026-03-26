package co.edu.uceva.usuariosservice.Auth.service;

import co.edu.uceva.usuariosservice.Auth.controller.AuthRequest;
import co.edu.uceva.usuariosservice.Auth.controller.LoginRequest;
import co.edu.uceva.usuariosservice.Auth.controller.TokenResponse;
import co.edu.uceva.usuariosservice.Auth.repository.ITokenRepository;
import co.edu.uceva.usuariosservice.Auth.repository.Token;
import co.edu.uceva.usuariosservice.domain.excepcions.UsuarioNoEncontradoException;
import co.edu.uceva.usuariosservice.domain.model.Usuario;
import co.edu.uceva.usuariosservice.domain.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;


import java.util.List;

@Service

public class AuthService {
    private final IUsuarioRepository usuarioRepository;
    private final ITokenRepository tokenRepository;
    private final JwtService jwtService; // Al estar en el mismo paquete 'service', no necesita import completo
    private final AuthenticationManager authenticationManager;

    public AuthService(IUsuarioRepository usuarioRepository,
                       ITokenRepository tokenRepository,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
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

        revokeAllUsuarioTokens(usuario);
        saveUsuarioToken(usuario, jwtToken);

        return new TokenResponse(jwtToken, jwtRefreshToken);
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
        revokeAllUsuarioTokens(usuario);
        saveUsuarioToken(usuario, accessToken);

        return new TokenResponse(accessToken, refreshToken);
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

        revokeAllUsuarioTokens(usuario);
        saveUsuarioToken(usuario, accessToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    private void saveUsuarioToken(Usuario usuario, String jwtToken) {
        Token token = new Token();
        token.setUsuario(usuario);
        token.setToken(jwtToken);
        token.setTokenType(Token.TokenType.BEARER);
        token.setExpired(false);
        token.setRevoked(false);

        tokenRepository.save(token);
    }

    private void revokeAllUsuarioTokens(final Usuario usuario) {
        final List<Token> validUserTokens = tokenRepository
                .findAllValidTokensByUser(usuario.getCodigo());

        if (!validUserTokens.isEmpty()) {
            validUserTokens.forEach(token -> {
                token.setExpired(true);
                token.setRevoked(true);
            });
            tokenRepository.saveAll(validUserTokens);
        }
    }
}
