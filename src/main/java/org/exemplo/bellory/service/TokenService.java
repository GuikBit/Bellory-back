package org.exemplo.bellory.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.exemplo.bellory.model.entity.users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class TokenService {

    @Value("${api.security.token.secret:my-secret-key}")
    private String secret;

    @Value("${api.security.token.expiration:36000}") // 10 horas em segundos
    private Long expirationTime;

    /**
     * Gera token JWT com username, userId, organizacaoId e role
     */
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("bellory-api")
                    .withSubject(user.getUsername())
                    .withClaim("userId", user.getId())
                    .withClaim("organizacaoId", user.getOrganizacao().getId())
                    .withClaim("role", user.getRole())
                    .withClaim("nomeCompleto", user.getNomeCompleto())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    /**
     * Valida o token e retorna o username
     */
    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    /**
     * Extrai o ID do usuário do token
     */
    public Long getUserIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("userId")
                    .asLong();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    /**
     * Extrai o ID da organização do token
     */
    public Long getOrganizacaoIdFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("organizacaoId")
                    .asLong();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    /**
     * Extrai a role do token
     */
    public String getRoleFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getClaim("role")
                    .asString();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    /**
     * Verifica se o token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            Instant expiration = JWT.require(algorithm)
                    .withIssuer("bellory-api")
                    .build()
                    .verify(token)
                    .getExpiresAt()
                    .toInstant();

            return expiration.isBefore(Instant.now());
        } catch (JWTVerificationException exception) {
            return true;
        }
    }

    /**
     * Renova o token (se ainda válido)
     */
    public String refreshToken(String token) {
        try {
            String username = validateToken(token);
            if (username == null || username.isEmpty()) {
                throw new RuntimeException("Token inválido");
            }

            Long userId = getUserIdFromToken(token);
            Long organizacaoId = getOrganizacaoIdFromToken(token);
            String role = getRoleFromToken(token);

            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("bellory-api")
                    .withSubject(username)
                    .withClaim("userId", userId)
                    .withClaim("organizacaoId", organizacaoId)
                    .withClaim("role", role)
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao renovar token", exception);
        }
    }

    /**
     * Retorna a data/hora de expiração do próximo token
     */
    public LocalDateTime getExpirationDateTime() {
        return LocalDateTime.ofInstant(genExpirationDate(), ZoneOffset.UTC);
    }

    /**
     * Gera a data de expiração
     */
    private Instant genExpirationDate() {
        return Instant.now().plusSeconds(expirationTime);
    }
}