package com.springboot.vitaltrail.infra.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// import jakarta.annotation.PostConstruct;

@Configuration
public class DotenvConfig {
    // private final Dotenv dotenv = Dotenv.load();

    // // Carga las variables de entorno del archivo .env en las propiedades del sistema
    // @PostConstruct
    // public void init() {
    //     dotenv.entries().forEach(entry -> {
    //         // System.out.println("Cargando variable: " + entry.getKey() + " = " + entry.getValue());
    //         if (System.getProperty(entry.getKey()) == null) {
    //             System.setProperty(entry.getKey(), entry.getValue());
    //         }
    //     });
    // }

    // @Bean
    // public Dotenv getDotenv() {
    //     return dotenv;
    // }

    @Bean
    public Dotenv dotenv() {
        return Dotenv.load(); // Esto carga automáticamente el archivo .env
    }
}
