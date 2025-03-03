package com.springboot.vitaltrail.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.springboot.vitaltrail.api.exception.CustomAccessDeniedHandler;
import com.springboot.vitaltrail.api.exception.CustomAuthenticationEntryPoint;
import com.springboot.vitaltrail.api.security.jwt.JWTAuthFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final JWTAuthFilter jwtAuthFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private static final String[] PUBLIC_READ_ENDPOINTS = {
            // "/courts/**",
            // "/courtsHours/**",
            // "/sports/**",
            // "/activities/**",
            // "/profiles/**",
            // "/hours/**",
            // "/months/**"
    };

    private static final String[] PUBLIC_WRITE_ENDPOINTS = {
            "/users/register", "/users/login",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Deshabilita la protección CSRF ya que utilizamos JWT en vez de cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Deshabilita las sesiones (para las APIs REST que usan tokens)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, PUBLIC_WRITE_ENDPOINTS).permitAll() // Rutas públicas de escritura
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_ENDPOINTS).permitAll() // Rutas públicas de lectura
                        .anyRequest().authenticated() // Autenticación requerida para otras rutas
                )
                // .anonymous(AbstractHttpConfigurer::disable) // Deshabilita acceso anónimo
                .exceptionHandling(handler -> handler
                    .accessDeniedHandler(customAccessDeniedHandler) // Manejo de acceso denegado (usuarios autenticados pero sin permisos)
                    .authenticationEntryPoint(customAuthenticationEntryPoint)) // Manejo de errores de autenticación (usuarios no autenticados)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Filtro JWT personalizado (middleware)

        return http.build();
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(); // Encripta y verifica contraseñas
    }

    // Manager de autenticación
    // Accede de forma predeterminada al AuthenticationProvider, que a su vez accede al UserDetailsService (userDetailsServiceImpl)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
