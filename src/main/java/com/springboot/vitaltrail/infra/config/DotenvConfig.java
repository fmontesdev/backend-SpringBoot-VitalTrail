package com.springboot.vitaltrail.infra.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvConfig {
    @Bean
    public Dotenv dotenv() {
        return Dotenv.load(); // Esto carga automáticamente el archivo .env
    }
}
