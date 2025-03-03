package com.springboot.vitaltrail.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.security.Keys; // Genera claves de firma para JWT
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets; // Define el conjunto de caracteres UTF-8
import java.security.Key; // Representa la clave de firma de un JWT

@Configuration
@AllArgsConstructor
public class JWTConfig {
    private final Dotenv dotenv;

    @Bean
    public Key accessTokenKey() {
        return Keys.hmacShaKeyFor(dotenv.get("ACCESS_TOKEN_SECRET").getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public Long accessTokenExpiration() {
        return Long.parseLong(dotenv.get("ACCESS_TOKEN_EXPIRATION"));
    }

    @Bean
    public Key refreshTokenKey() {
        return Keys.hmacShaKeyFor(dotenv.get("REFRESH_TOKEN_SECRET").getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public Long refreshTokenExpiration() {
        return Long.parseLong(dotenv.get("REFRESH_TOKEN_EXPIRATION"));
    }
}
