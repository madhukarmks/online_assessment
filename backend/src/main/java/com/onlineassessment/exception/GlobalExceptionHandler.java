package com.onlineassessment.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(
            boolean success,
            String message,
            String timestamp,
            List<Object> errors
    ) {}

    @ExceptionHandler(Exceptions.NotFound.class)
    ResponseEntity<ErrorResponse> notFound(Exceptions.NotFound exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exceptions.BadRequest.class)
    ResponseEntity<ErrorResponse> badRequest(Exceptions.BadRequest exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exceptions.Forbidden.class)
    ResponseEntity<ErrorResponse> forbidden(Exceptions.Forbidden exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        List<Object> errors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fieldError.getField());
            item.put("message", fieldError.getDefaultMessage());
            errors.add(item);
        });
        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> database(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Request could not be completed", List.of());
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            List<Object> errors) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(
                        false,
                        message,
                        Instant.now().toString(),
                        errors
                )
        );
    }
}
