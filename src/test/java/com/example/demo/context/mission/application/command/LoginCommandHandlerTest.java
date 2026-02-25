package com.example.demo.context.mission.application.command;

import com.example.demo.context.mission.application.port.UserActionEventPublisher;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionInitializationService;
import com.example.demo.context.mission.domain.event.UserLoggedInEvent;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock
    UserQueryPort userQueryPort;

    @Mock
    MissionInitializationService missionInitializationService;

    @Mock
    UserActionEventPublisher userActionEventPublisher;

    @InjectMocks
    LoginCommandHandler handler;

    private static final Long USER_ID = 1L;
    private static final LocalDate LOGIN_DATE = LocalDate.of(2026, 1, 10);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void handle_publishesUserLoggedInEvent() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));

        handler.handle(new LoginCommand(USER_ID, LOGIN_DATE, 0L));

        ArgumentCaptor<UserLoggedInEvent> captor = ArgumentCaptor.forClass(UserLoggedInEvent.class);
        verify(userActionEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().loginDate()).isEqualTo(LOGIN_DATE);
    }

    @Test
    void handle_initializesMissionsWithCreatedAtPlus30Days() {
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));

        handler.handle(new LoginCommand(USER_ID, LOGIN_DATE, 0L));

        verify(missionInitializationService).ensureMissionsExist(USER_ID, CREATED_AT.plusDays(30));
    }

    @Test
    void handle_throwsUserNotFoundWhenUserDoesNotExist() {
        when(userQueryPort.getUserCreatedAt(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new LoginCommand(99L, LOGIN_DATE, 0L)))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void handle_doesNotPublishWhenUserNotFound() {
        when(userQueryPort.getUserCreatedAt(99L)).thenReturn(Optional.empty());

        try {
            handler.handle(new LoginCommand(99L, LOGIN_DATE, 0L));
        } catch (UserNotFoundException ignored) {
        }

        verify(userActionEventPublisher, never()).publish(any());
    }

    @Test
    void handle_preservesOccurredAtFromCommand() {
        long occurredAt = 1_700_000_000_000L;
        when(userQueryPort.getUserCreatedAt(USER_ID)).thenReturn(Optional.of(CREATED_AT));

        handler.handle(new LoginCommand(USER_ID, LOGIN_DATE, occurredAt));

        ArgumentCaptor<UserLoggedInEvent> captor = ArgumentCaptor.forClass(UserLoggedInEvent.class);
        verify(userActionEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().occurredAt()).isEqualTo(occurredAt);
    }
}
