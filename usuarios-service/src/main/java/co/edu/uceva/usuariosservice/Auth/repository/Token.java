package co.edu.uceva.usuariosservice.Auth.repository;

import co.edu.uceva.usuariosservice.domain.model.Usuario;
import jakarta.persistence.*;

@Entity
@Table(name = "tokens")
public class Token {

        public enum TokenType { BEARER }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(unique = true)
        private String token;

        @Enumerated(EnumType.STRING)
        private TokenType tokenType = TokenType.BEARER;

        private boolean revoked;

        private boolean expired;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "codigo") // Asegúrate de que este sea el nombre de columna correcto para tu BD
        private Usuario usuario;

        // --- CONSTRUCTORES ---
        public Token() {
        }

        // --- GETTERS Y SETTERS ---
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public TokenType getTokenType() { return tokenType; }
        public void setTokenType(TokenType tokenType) { this.tokenType = tokenType; }

        public boolean isRevoked() { return revoked; }
        public void setRevoked(boolean revoked) { this.revoked = revoked; }

        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }

        public Usuario getUsuario() { return usuario; }
        public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}