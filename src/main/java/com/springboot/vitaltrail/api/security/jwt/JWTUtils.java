package com.springboot.vitaltrail.api.security.jwt;

import io.jsonwebtoken.Claims; // Representa el Payload de un JWT
import io.jsonwebtoken.Jwts; // Para creación y validación de JWT

 // Para generar excepciones de JWTs no válidos
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;

import java.security.Key; // Representa la clave de firma de un JWT
import java.time.Instant; // Maneja marcas de tiempo actuales en UTC
import java.util.Date; // Para trabajar con fechas tradicionales en Java
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.domain.user.UserEntity.Role;

import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JWTUtils {
    private final Key accessTokenKey;
    private final Long accessTokenExpiration;
    private final Key refreshTokenKey;
    private final Long refreshTokenExpiration;

    // Constructor
    @Autowired
    public JWTUtils(
        Key accessTokenKey,
        Long accessTokenExpiration,
        Key refreshTokenKey,
        Long refreshTokenExpiration
    ) {
        this.accessTokenKey = accessTokenKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenKey = refreshTokenKey;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // Crea JWT
    public String generateJWT(UUID idUser, String email, String username, Role rol, String tokenType) {
        if (idUser == null || email == null || email.isEmpty() || username == null || username.isEmpty() || rol == null) {
            throw new AppException(Error.ILLEGAL_ARGUMENT_TOKEN);
        }

        Key key = "access".equals(tokenType) ? accessTokenKey : refreshTokenKey;
        Long expiration = "access".equals(tokenType) ? accessTokenExpiration : refreshTokenExpiration;

        Instant exp = Instant.now(); // Momento actual
        return Jwts.builder()
            .setSubject(idUser.toString())
            .claim("email", email)
            .claim("username", username)
            .claim("rol", rol)
            .setIssuedAt(new Date(exp.toEpochMilli()))
            .setExpiration(new Date(exp.toEpochMilli() + expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact(); // Genera JWT serializado
    }

    // Valida JWT
    public boolean validateJWT(String jwt, String tokenType) {
        try {
            Key key = "access".equals(tokenType) ? accessTokenKey : refreshTokenKey;

            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt) // Analiza y valida el token JWT. Si el token es inválido lanza una JwtException
                .getBody();
            Instant now = Instant.now(); // Momento actual
            Date exp = claims.getExpiration(); // Recupera la fecha de expiración del token
            return exp.after(Date.from(now)); // Devuelve boolean dependiendo si la f.expiración es posterior al momento actual

        } catch (SignatureException e) {
            throw new AppException(Error.INVALID_TOKEN_SIGNATURE);
        } catch (MalformedJwtException e) {
            throw new AppException(Error.MALFORMED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new AppException(Error.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new AppException(Error.ILLEGAL_ARGUMENT_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new AppException(Error.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new AppException(Error.INVALID_TOKEN);
        }
    }

    // Extrae el sub (identificador) de un JWT válido
    public String validateJwtAndgetEmail(String jwt, String tokenType) {
        try {
            Key key = "access".equals(tokenType) ? accessTokenKey : refreshTokenKey;
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

            return claims.get("email", String.class);

        } catch (SignatureException e) {
            throw new AppException(Error.INVALID_TOKEN_SIGNATURE);
        } catch (MalformedJwtException e) {
            throw new AppException(Error.MALFORMED_TOKEN);
        } catch (UnsupportedJwtException e) {
            throw new AppException(Error.UNSUPPORTED_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new AppException(Error.ILLEGAL_ARGUMENT_TOKEN);
        } catch (ExpiredJwtException e) {
            throw e; // Propaga esta excepción para manejarla en el filtro
        } catch (JwtException e) {
            throw new AppException(Error.INVALID_TOKEN);
        }
    }
}
