package com.example.demo.context.mission.application.command;

import com.example.demo.common.cqrs.command.Command;

public record PlayGameCommand(
    Long userId,
    Long gameId,
    int score,
    String idempotencyKey,
    long occurredAt
) implements Command<Void> {
}
