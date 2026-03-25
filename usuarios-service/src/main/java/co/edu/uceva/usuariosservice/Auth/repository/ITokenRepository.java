package co.edu.uceva.usuariosservice.Auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ITokenRepository extends JpaRepository<Token, Long> {
    @Query("""
        select t from Token t 
        where t.usuario.codigo = :usuarioId 
        and (t.expired = false or t.revoked = false)
    """)
    List<Token> findAllValidTokensByUser(@Param("usuarioId") Long usuarioId);

    Optional<Token> findByToken(String token);
}