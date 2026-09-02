package com.leang.authservice.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequestException(BadRequestException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setProperty("timestamp", Instant.now());
        if (e.hasFieldErrors()) {
            problemDetail.setProperty("fieldErrors", e.getFieldErrors());
        }
        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbiddenException(ForbiddenException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle("Forbidden");
        problemDetail.setDetail(e.getMessage());
        problemDetail.setProperty("timestamp", java.time.OffsetDateTime.now());

        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e) {
        return e.toProblemDetail();
    }


    //     Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        // Collect field validation errors
        Map<String, String> errors = new HashMap<>();
        int blankFieldErrors = 0;

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());

            // Check if the error is specifically "must not be blank"
            if ("must not be blank".equals(fieldError.getDefaultMessage())) {
                blankFieldErrors++;
            }
        }

        // Add errors to ProblemDetail as extra properties
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }


    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidationException(HandlerMethodValidationException e) {
        Map<String, String> errors = new HashMap<>();

        // Loop through each invalid parameter validation result
        e.getParameterValidationResults().forEach(parameterError -> {
            String paramName = parameterError.getMethodParameter().getParameterName(); // Get parameter name

            // Loop through each validation error message for this parameter
            for (var messageError : parameterError.getResolvableErrors()) {
                errors.put(paramName, messageError.getDefaultMessage()); // Store error message
            }
        });

        // Create structured ProblemDetail response
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Method Parameter Validation Failed");
        problemDetail.setProperties(Map.of("timestamp", LocalDateTime.now(), "errors", errors // Attach validation errors
        ));

        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException invalidFormatEx) {
            Class<?> targetType = invalidFormatEx.getTargetType();

            if (targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();
                String allowedValues = Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));

                String invalidValue = String.valueOf(invalidFormatEx.getValue());
                ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
                problem.setTitle("Bad Request");
                problem.setDetail(String.format("JSON parse error: Cannot deserialize value of type '%s' from String \"%s\": not one of the values accepted for Enum class: [%s]",
                        targetType.getName(), invalidValue, allowedValues));
                problem.setProperty("timestamp", OffsetDateTime.now());
                return problem;
            }
        }

        // Fallback for other JSON parsing errors
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Invalid request body.");
        problem.setProperty("timestamp", OffsetDateTime.now());
        return problem;
    }


}