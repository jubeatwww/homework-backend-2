package com.example.demo.context.mission.interfaces.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayGameRequest(
    @NotNull Long userId,
    @NotNull Long gameId,
    @NotNull @Min(0) Integer score,
    @Size(max = 100) String idempotencyKey,
    Long occurredAt
) {
}
