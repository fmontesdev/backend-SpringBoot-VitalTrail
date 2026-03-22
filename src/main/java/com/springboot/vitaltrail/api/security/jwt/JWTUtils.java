package com.springboot.vitaltrail.api.security.jwt;

import io.jsonwebtoken.Claims; // Representa el Payload de un JWT
import io.jsonwebtoken.Jwts; // Para creación y validación de JWT

 // Para generar excepciones de JWTs no válidos
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;

import java.security.Key; // Representa la clave de firma de un JWT

import org.springframework.stereotype.Component;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JWTUtils {
    private final Key accessTokenKey;

    // Constructor
    @Autowired
    public JWTUtils(Key accessTokenKey) {
        this.accessTokenKey = accessTokenKey;
    }

    // Extrae el email de un JWT de acceso válido (firmado con el secret de Symfony)
    public String validateJwtAndgetEmail(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessTokenKey)
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
