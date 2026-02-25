package com.example.demo.context.mission.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

public record LaunchGameRequest(@NotNull Long userId, @NotNull Long gameId, Long occurredAt) {
}