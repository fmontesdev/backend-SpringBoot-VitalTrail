package com.springboot.vitaltrail.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum Error {
    // Errores de autenticación y autorización
    UNAUTHORIZED("Falta de credenciales o token inválido", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("Acceso denegado", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("Token inválido", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_SIGNATURE("Firma del Token inválida", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("Token expirado", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("Token mal formado", HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_TOKEN("Token no compatible", HttpStatus.UNAUTHORIZED),
    ILLEGAL_ARGUMENT_TOKEN("Token está vacio o es nulo", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_BOOKING("El usuario actual no es el autor de la reserva", HttpStatus.UNAUTHORIZED),

    // Errores de validación
    INVALID_INPUT("Entrada inválida", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_USER("Tipo de usuario inválido", HttpStatus.BAD_REQUEST),

    // Errores de negocio
    DUPLICATED_USERNAME("El nombre de usuario ya está en uso. Introduce otro", HttpStatus.CONFLICT),
    DUPLICATED_EMAIL("El correo electrónico ya está en uso. Introduce otro", HttpStatus.CONFLICT),

    // Errores de recursos no encontrados
    USER_NOT_FOUND("Usuario no encontrado", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND("Suscripción no encontrada", HttpStatus.NOT_FOUND),

    // Error genérico del servidor
    INTERNAL_SERVER_ERROR("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("Error de conexión", HttpStatus.SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus status;
}
