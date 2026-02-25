package com.example.demo.context.mission.application.command;

import com.example.demo.common.cqrs.command.CommandHandler;
import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserActionEventPublisher;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionInitializationService;
import com.example.demo.context.mission.domain.event.GameLaunchedEvent;
import com.example.demo.context.mission.domain.exception.GameNotFoundException;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LaunchGameCommandHandler implements CommandHandler<LaunchGameCommand, Void> {

    private final UserQueryPort userQueryPort;
    private final GameQueryPort gameQueryPort;
    private final MissionInitializationService missionInitializationService;
    private final UserActionEventPublisher userActionEventPublisher;

    @Override
    public Void handle(LaunchGameCommand command) {
        LocalDateTime createdAt = userQueryPort.getUserCreatedAt(command.userId())
            .orElseThrow(() -> new UserNotFoundException(command.userId()));
        if (!gameQueryPort.gameExists(command.gameId())) {
            throw new GameNotFoundException(command.gameId());
        }
        missionInitializationService.ensureMissionsExist(command.userId(), createdAt.plusDays(30));
        userActionEventPublisher.publish(new GameLaunchedEvent(command.userId(), command.gameId(), command.occurredAt()));
        return null;
    }
}
