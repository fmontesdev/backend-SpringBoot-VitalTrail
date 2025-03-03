package com.springboot.vitaltrail.domain.exception;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {
    private final int statusCode;
    private final String details;

    public NotificationException(String message, int statusCode, String details) {
        super(message);
        this.statusCode = statusCode;
        this.details = details;
    }

    public NotificationException(String message, Throwable cause, int statusCode, String details) {
        super(message, cause);
        this.statusCode = statusCode;
        this.details = details;
    }
}
