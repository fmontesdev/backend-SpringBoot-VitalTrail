package com.springboot.vitaltrail.api.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ErrorMessages {
    private final Map<String, String> errors;

    public ErrorMessages() {
        errors = new HashMap<>();
    }

    public void addError(String idError, String message) {
        errors.put(idError, message);
    }
}
