package com.example.demo.context.mission.domain.exception;

import com.example.demo.context.shared.domain.DomainException;

import java.util.List;

public class IdempotencyKeyRequiredException extends DomainException {

    public IdempotencyKeyRequiredException() {
        super("IDEMPOTENCY_KEY_REQUIRED", List.of("X-Idempotency-Key", "idempotencyKey"));
    }
}
