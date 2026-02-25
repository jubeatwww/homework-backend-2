package com.example.demo.common.cqrs.command;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Set;

public interface SelfValidating {
    default void validateSelf() {
        Set<ConstraintViolation<Object>> violations = ValidatorHolder.get().validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
