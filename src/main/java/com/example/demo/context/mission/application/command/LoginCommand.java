package com.example.demo.context.mission.application.command;

import com.example.demo.common.cqrs.command.Command;

import java.time.LocalDate;

public record LoginCommand(
    Long userId,
    LocalDate loginDate,
    long occurredAt
) implements Command<Void> {
}
