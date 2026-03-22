package com.springboot.vitaltrail.api.security.jwt;

import com.springboot.vitaltrail.api.security.AuthUtils;
import com.springboot.vitaltrail.api.security.UserDetailsServiceImpl;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {
    private final JWTUtils jwtUtils;
    private final AuthUtils authUtils;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final Logger logger = LoggerFactory.getLogger(JWTAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin cabecera Authorization o sin prefijo Bearer: continúa sin autenticar
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String accessToken = authHeader.substring(TOKEN_PREFIX.length());

        try {
            // Valida el access token (firmado con el secret de Symfony) y extrae el email
            String email = jwtUtils.validateJwtAndgetEmail(accessToken);

            // Si el token es válido y el usuario no está autenticado, configura la autenticación en el contexto de seguridad
            if (email != null && !authUtils.isAuthenticated()) {
                configureAuthentication(request, email);
            }

            // Continúa con la cadena de filtros
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // El token expiró: Symfony gestiona la renovación, devolvemos 401
            logger.warn("Access token expirado para sub: {}", e.getClaims().getSubject());
            request.setAttribute("APP_EXCEPTION", new AppException(Error.EXPIRED_TOKEN));
            filterChain.doFilter(request, response);

        } catch (AppException e) {
            logger.error("Token inválido: {}", e.getMessage());
            request.setAttribute("APP_EXCEPTION", e);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Excepción inesperada durante la validación del token", e);
            request.setAttribute("APP_EXCEPTION", new AppException(Error.UNAUTHORIZED));
            filterChain.doFilter(request, response);
        }
    }

    private void configureAuthentication(HttpServletRequest request, String email) {
        var userDetails = userDetailsServiceImpl.loadUserByUsername(email);

        if (userDetails != null) {
            var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
