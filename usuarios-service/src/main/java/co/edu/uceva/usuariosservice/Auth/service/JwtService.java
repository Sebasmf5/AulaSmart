package co.edu.uceva.usuariosservice.Auth.service;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long jwtRefreshExpiration;

    // Extrae el código (ID) del usuario guardado en el ID del token
    public Long extractCodigo(final String token) {
        return Long.parseLong(getAllClaims(token).getId());
    }

    public String extractRol(final String token) {
        return getAllClaims(token).get("rol", String.class);
    }

    public boolean isTokenValid(String token, Usuario usuario) {
        final Long codigo = extractCodigo(token);
        return (codigo.equals(usuario.getCodigo())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return getAllClaims(token).getExpiration().before(new Date());
    }

    public String generateToken(final Usuario usuario) {
        return buildToken(usuario, jwtExpiration);
    }

    public String generateRefreshToken(final Usuario usuario) {
        return buildToken(usuario, jwtRefreshExpiration);
    }

    private String buildToken(final Usuario usuario, final long expiration) {
        return Jwts.builder()
                .id(usuario.getCodigo().toString()) // jti
                .subject(usuario.getEmail())       // sub
                .claim("nombre", usuario.getNombre() + " " + usuario.getApellido()) // claim personalizado
                .claim("rol", usuario.getRol())                                     // claim personalizado
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}