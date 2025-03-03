package com.springboot.vitaltrail.api.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.NotificationException;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class AppExceptionHandler {
    // Maneja errores genéricos
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorMessages> handleGenericException(Exception exception) {
        return responseErrorMessages(
                Map.of("INTERNAL_SERVER_ERROR", "Error interno del servidor"), // Error code, Error message
                HttpStatus.INTERNAL_SERVER_ERROR // Error http status
        );
    }

    // Maneja errores específicos
    @ExceptionHandler({AppException.class, AuthenticationException.class, AccessDeniedException.class})
    @ResponseBody
    public ResponseEntity<ErrorMessages> handleAppException(AppException exception) {
        return responseErrorMessages(
            Map.of(exception.getError().name(), exception.getMessage()),
            exception.getError().getStatus()
        );
    }

    // Maneja errores del servidor de notificaciones
    @ExceptionHandler(NotificationException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessages> handleNotificationException(NotificationException exception) {
        return responseErrorMessages(
            Map.of("NOTIFICATION_ERROR", exception.getMessage() + " - " + exception.getDetails()),
            HttpStatus.valueOf(exception.getStatusCode())
        );
    }

    // Maneja errores de validación en los DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorMessages> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> fieldError = exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField, // Error field
                FieldError::getDefaultMessage // Error message
            ));
        return responseErrorMessages(fieldError, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private ResponseEntity<ErrorMessages> responseErrorMessages(Map<String, String> error, HttpStatus status) {
        ErrorMessages errorMessages = new ErrorMessages();
        error.forEach(errorMessages::addError);
        return new ResponseEntity<>(errorMessages, status);
    }
}
