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
    BLACKLISTED_TOKEN("Refresh Token ya existe en la Blacklist", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("Token inválido", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_SIGNATURE("Firma del Token inválida", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("Token expirado", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("Token mal formado", HttpStatus.UNAUTHORIZED),
    UNSUPPORTED_TOKEN("Token no compatible", HttpStatus.UNAUTHORIZED),
    ILLEGAL_ARGUMENT_TOKEN("Token está vacio o es nulo", HttpStatus.UNAUTHORIZED),
    PASSWORD_INVALID("La contraseña en el login no es válida", HttpStatus.UNAUTHORIZED),
    LOGIN_INFO_INVALID("Los datos del login no son correctos", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_BOOKING("El usuario actual no es el autor de la reserva", HttpStatus.UNAUTHORIZED),

    // Errores de validación
    INVALID_INPUT("Entrada inválida", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_USER("Tipo de usuario inválido", HttpStatus.BAD_REQUEST),

    // Errores de negocio
    DUPLICATED_USERNAME("El nombre de usuario ya está en uso. Introduce otro", HttpStatus.CONFLICT),
    DUPLICATED_EMAIL("El correo electrónico ya está en uso. Introduce otro", HttpStatus.CONFLICT),
    ACTIVITY_NOT_AVAILABLE("No quedan plazas en la actividad", HttpStatus.CONFLICT),
    USER_ALREADY_INSCRIBED("Usuario ya inscrito en la actividad", HttpStatus.CONFLICT),
    ALREADY_FOLLOWED_USER("Usuario ya seguido", HttpStatus.CONFLICT),
    // ALREADY_FAVORITED_ARTICLE("already followed user", HttpStatus.UNPROCESSABLE_ENTITY),

    // Errores de recursos no encontrados
    USER_NOT_FOUND("Usuario no encontrado", HttpStatus.NOT_FOUND),
    COURT_NOT_FOUND("Pista no encontrada", HttpStatus.NOT_FOUND),
    COURT_HOUR_NOT_FOUND("Horario de pista no encontrado", HttpStatus.NOT_FOUND),
    COURT_HOUR_NOT_AVAILABLE("Horario de pista no disponible", HttpStatus.NOT_FOUND),
    SPORT_NOT_FOUND("Deporte no encontrado", HttpStatus.NOT_FOUND),
    ACTIVITY_NOT_FOUND("Actividad no encontrada", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("Comentario no encontrado", HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND("Reserva de pista no encontrada", HttpStatus.NOT_FOUND),
    INSCRIPTION_NOT_FOUND("Inscripción de actividad no encontrada", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND("Refresh Token no encontrado", HttpStatus.NOT_FOUND),
    BLACKLIST_TOKEN_NOT_FOUND("Blacklist Token no encontrado", HttpStatus.NOT_FOUND),
    FOLLOW_NOT_FOUND("Usuario seguido no encontrado", HttpStatus.NOT_FOUND),
    // FAVORITE_NOT_FOUND("favorite not found", HttpStatus.NOT_FOUND),

    // Error genérico del servidor
    INTERNAL_SERVER_ERROR("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("Error de conexión", HttpStatus.SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus status;
}
