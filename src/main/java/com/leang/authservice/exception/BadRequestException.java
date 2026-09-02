package com.leang.authservice.exception;

import java.util.Map;

public class BadRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public BadRequestException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public BadRequestException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
