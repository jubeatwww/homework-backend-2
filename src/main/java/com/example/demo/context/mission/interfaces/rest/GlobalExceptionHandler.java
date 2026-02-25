package com.example.demo.context.mission.interfaces.rest;

import com.example.demo.context.mission.domain.exception.GameNotFoundException;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import com.example.demo.context.shared.domain.DomainException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "code", ex.getCode(),
            "arguments", ex.getArguments()
        ));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "code", ex.getCode(),
            "arguments", ex.getArguments()
        ));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomainException(DomainException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "code", ex.getCode(),
            "arguments", ex.getArguments()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (a, b) -> a
            ));
        return ResponseEntity.badRequest().body(Map.of(
            "code", "VALIDATION_FAILED",
            "errors", errors
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (a, b) -> a
            ));
        return ResponseEntity.badRequest().body(Map.of(
            "code", "VALIDATION_FAILED",
            "errors", errors
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "code", "VALIDATION_FAILED",
            "errors", Map.of(ex.getParameterName(), "parameter is required")
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of(
            "code", "INTERNAL_ERROR"
        ));
    }
}
