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

    @Value("${security.jwt.signing-key}")
    private String secret;

    @Value("${security.jwt.expiration-hours:2}")
    private int expirationHours;

    private static final String ISSUER = "Bellory-API";

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getUsername())
                    .withClaim("userId", user.getId()) // Assumindo que User tem getId()
                    .withClaim("roles", user.getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .collect(Collectors.toList()))
                    .withIssuedAt(new Date())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);

        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar token JWT: " + exception.getMessage(), exception);
        }
    }

    public String validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token.trim());

            return jwt.getSubject();

        } catch (JWTVerificationException exception) {
            System.err.println("Token validation failed: " + exception.getMessage());
            return null; // Token inválido
        }
    }

    public DecodedJWT decodeToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            if (jwt == null) {
                return true;
            }
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = decodeToken(token);
            if (jwt != null) {
                return jwt.getClaim("userId").asLong();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String refreshToken(String oldToken) {
        try {
            DecodedJWT jwt = decodeToken(oldToken);
            if (jwt == null) {
                throw new RuntimeException("Token inválido para refresh");
            }

            // Verificar se o token ainda tem pelo menos 15 minutos de vida
            long fifteenMinutesInMs = 15 * 60 * 1000;
            if (jwt.getExpiresAt().getTime() - System.currentTimeMillis() > fifteenMinutesInMs) {
                throw new RuntimeException("Token ainda válido, refresh não necessário");
            }

            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(jwt.getSubject())
                    .withClaim("userId", jwt.getClaim("userId").asLong())
                    .withClaim("roles", jwt.getClaim("roles").asList(String.class))
                    .withIssuedAt(new Date())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao fazer refresh do token: " + exception.getMessage(), exception);
        }
    }

    private Instant generateExpirationDate() {
        return LocalDateTime.now()
                .plusHours(expirationHours)
                .toInstant(ZoneOffset.of("-03:00"));
    }

    public LocalDateTime getExpirationDateTime() {
        return LocalDateTime.now().plusHours(expirationHours);
    }
}