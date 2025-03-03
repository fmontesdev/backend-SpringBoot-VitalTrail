package com.springboot.vitaltrail.domain.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final Error error;
    
    public AppException(Error error) {
        super(error.getMessage());
        this.error = error;
    }

    public AppException(Error error, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
    }
}
