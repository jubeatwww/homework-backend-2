package com.example.demo.context.mission.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LoginRequest(@NotNull Long userId, @NotNull LocalDate loginDate, Long occurredAt) {
}
