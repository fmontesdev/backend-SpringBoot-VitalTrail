package com.springboot.vitaltrail.api.security.jwt;

import com.springboot.vitaltrail.api.security.AuthUtils;
import com.springboot.vitaltrail.api.security.UserDetailsServiceImpl;
import com.springboot.vitaltrail.domain.blacklistToken.BlacklistTokenService;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.domain.refreshToken.RefreshTokenService;
import com.springboot.vitaltrail.domain.user.UserEntity.Role;

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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {
    private final JWTUtils jwtUtils;
    private final AuthUtils authUtils;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistTokenService blacklistTokenService;
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final Logger logger = LoggerFactory.getLogger(JWTAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Verificación del autheader
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Obtención del access token
        final String accessToken = authHeader.substring(TOKEN_PREFIX.length());

        try {
            validateAccessToken(request, response, filterChain, accessToken);
        } catch (ExpiredJwtException e) {
            handleExpiredToken(request, response, filterChain, e);
        } catch (AppException e) {
            logger.error("AppException durante la validación del token: {}", e.getMessage());
            request.setAttribute("APP_EXCEPTION", e);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Excepción inesperada durante la validación del token", e);
            request.setAttribute("APP_EXCEPTION", new AppException(Error.UNAUTHORIZED));
            filterChain.doFilter(request, response);
        }
    }

    private void validateAccessToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String accessToken) throws IOException, ServletException {
        // Validación del token y obtención del email
        String email = jwtUtils.validateJwtAndgetEmail(accessToken, "access");

        // Si el access token es valido y el contexto de seguridad no esta configurado, establece la autenticación
        if (email != null && !authUtils.isAuthenticated()) {
            configureAuthentication(request, email);
        }

        filterChain.doFilter(request, response);
    }

    private void handleExpiredToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, ExpiredJwtException e) throws IOException, ServletException {
        final String idUser = e.getClaims().getSubject();
        final String expiredAccessTokenEmail = e.getClaims().get("email", String.class);
        final String rol = e.getClaims().get("rol", String.class);

        if (!"admin".equals(rol)) {
            // Busca el refreshToken asociado
            final String refreshToken = getRefreshToken(idUser);
            // logger.info("Refresh Token: {}", refreshToken);

            // Verifica que el refreshToken no esté en la blacklist
            if (blacklistTokenService.isBlacklisted(refreshToken)) {
                logger.warn("Intento de uso de Blacklist Token: {}", refreshToken);
                handleAppException(request, response, filterChain, new AppException(Error.BLACKLISTED_TOKEN));
                return;
            }

            // Valida el Refresh Token
            try {
                if (jwtUtils.validateJWT(refreshToken, "refresh")) {
                    final String refreshTokenEmail = jwtUtils.validateJwtAndgetEmail(refreshToken, "refresh");
                    if (!refreshTokenEmail.equals(expiredAccessTokenEmail)) {
                        logger.warn("Refresh Token inválido: {}", refreshToken);
                        handleAppException(request, response, filterChain, new AppException(Error.INVALID_TOKEN));
                        return;
                    }
                }
            } catch (AppException ex) {
                if (!"admin".equals(rol)) blacklistTokenService.saveBlacklistToken(refreshToken);
                logger.warn("Refresh Token inválido: {}", refreshToken);
                handleAppException(request, response, filterChain, ex);
                return;
            }

            // Genera un nuevo access token
            var newAccessToken = jwtUtils.generateJWT(
                UUID.fromString(idUser),
                e.getClaims().get("email", String.class),
                e.getClaims().get("username", String.class),
                Role.valueOf(rol),
                "access"
            );
            logger.info("New Access Token generado: {}", newAccessToken);

            // Configura la autenticación
            configureAuthentication(request, expiredAccessTokenEmail);

            // Agrega el nuevo Access Token como encabezado
            response.setHeader("New-Access-Token", newAccessToken);

            // Continúa con el flujo normal
            filterChain.doFilter(request, response);
        }
    }

    private void configureAuthentication(HttpServletRequest request, String email) {
        // Carga los detalles del usuario
        var userDetails = userDetailsServiceImpl.loadUserByUsername(email);
        
        // Establece la autenticación en el contexto de seguridad
        if (userDetails != null) {
            var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
    
    private void handleAppException(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, AppException e) throws IOException, ServletException {
        request.setAttribute("APP_EXCEPTION", e);
        filterChain.doFilter(request, response);
    }

    private String getRefreshToken(String idUser) {
        var refreshTokenEntity = refreshTokenService.getRefreshToken(UUID.fromString(idUser));
        return refreshTokenEntity.getRefreshToken();
    }
}
