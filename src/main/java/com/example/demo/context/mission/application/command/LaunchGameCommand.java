package com.example.demo.context.mission.application.command;

import com.example.demo.common.cqrs.command.Command;

public record LaunchGameCommand(
    Long userId,
    Long gameId,
    long occurredAt
) implements Command<Void> {
}
