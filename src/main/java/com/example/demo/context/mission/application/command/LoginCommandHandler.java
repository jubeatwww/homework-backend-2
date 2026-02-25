package com.example.demo.context.mission.application.command;

import com.example.demo.common.cqrs.command.CommandHandler;
import com.example.demo.context.mission.application.port.UserActionEventPublisher;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionInitializationService;
import com.example.demo.context.mission.domain.event.UserLoggedInEvent;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginCommandHandler implements CommandHandler<LoginCommand, Void> {

    private final UserQueryPort userQueryPort;
    private final MissionInitializationService missionInitializationService;
    private final UserActionEventPublisher userActionEventPublisher;

    @Override
    public Void handle(LoginCommand command) {
        LocalDateTime createdAt = userQueryPort.getUserCreatedAt(command.userId())
            .orElseThrow(() -> new UserNotFoundException(command.userId()));
        missionInitializationService.ensureMissionsExist(command.userId(), createdAt.plusDays(30));
        userActionEventPublisher.publish(new UserLoggedInEvent(command.userId(), command.loginDate(), command.occurredAt()));
        return null;
    }
}
