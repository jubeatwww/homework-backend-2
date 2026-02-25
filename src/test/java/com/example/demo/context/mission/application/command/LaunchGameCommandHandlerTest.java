package com.example.demo.context.mission.application.command;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserActionEventPublisher;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionInitializationService;
import com.example.demo.context.mission.domain.event.GameLaunchedEvent;
import com.example.demo.context.mission.domain.exception.GameNotFoundException;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaunchGameCommandHandlerTest {

    @Mock
    UserQueryPort userQueryPort;

    @Mock
    GameQueryPort gameQueryPort;

    @Mock
    MissionInitializationService missionInitializationService;

    @Mock
    UserActionEventPublisher userActionEventPublisher;

    @InjectMocks
    LaunchGameCommandHandler handler;

    private static final Long USER_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void handle_publishesGameLaunchedEvent() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));
        when(gameQueryPort.gameExists(GAME_ID)).thenReturn(true);

        handler.handle(new LaunchGameCommand(USER_ID, GAME_ID, 0L));

        ArgumentCaptor<GameLaunchedEvent> captor = ArgumentCaptor.forClass(GameLaunchedEvent.class);
        verify(userActionEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().gameId()).isEqualTo(GAME_ID);
    }

    @Test
    void handle_initializesMissionsWithCreatedAtPlus30Days() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));
        when(gameQueryPort.gameExists(GAME_ID)).thenReturn(true);

        handler.handle(new LaunchGameCommand(USER_ID, GAME_ID, 0L));

        verify(missionInitializationService).ensureMissionsExist(USER_ID, CREATED_AT.plusDays(30));
    }

    @Test
    void handle_throwsUserNotFoundWhenUserDoesNotExist() {
        when(userQueryPort.getUserCreatedAt(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new LaunchGameCommand(99L, GAME_ID, 0L)))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void handle_throwsGameNotFoundWhenGameDoesNotExist() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));
        when(gameQueryPort.gameExists(99L)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new LaunchGameCommand(USER_ID, 99L, 0L)))
            .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void handle_doesNotPublishWhenUserNotFound() {
        when(userQueryPort.getUserCreatedAt(99L)).thenReturn(Optional.empty());

        try {
            handler.handle(new LaunchGameCommand(99L, GAME_ID, 0L));
        } catch (UserNotFoundException ignored) {
        }

        verify(userActionEventPublisher, never()).publish(any());
    }

    @Test
    void handle_doesNotPublishWhenGameNotFound() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));
        when(gameQueryPort.gameExists(99L)).thenReturn(false);

        try {
            handler.handle(new LaunchGameCommand(USER_ID, 99L, 0L));
        } catch (GameNotFoundException ignored) {
        }

        verify(userActionEventPublisher, never()).publish(any());
    }

    @Test
    void handle_preservesOccurredAtFromCommand() {
        long occurredAt = 1_700_000_000_000L;
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));
        when(gameQueryPort.gameExists(GAME_ID)).thenReturn(true);

        handler.handle(new LaunchGameCommand(USER_ID, GAME_ID, occurredAt));

        ArgumentCaptor<GameLaunchedEvent> captor = ArgumentCaptor.forClass(GameLaunchedEvent.class);
        verify(userActionEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().occurredAt()).isEqualTo(occurredAt);
    }
}
