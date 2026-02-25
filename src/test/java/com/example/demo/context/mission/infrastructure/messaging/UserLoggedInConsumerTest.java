package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.UserLoggedInEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLoggedInConsumerTest {

    @Mock UserQueryPort userQueryPort;
    @Mock UserEligibilityService userEligibilityService;
    @Mock MissionProgressService missionProgressService;
    @InjectMocks UserLoggedInConsumer consumer;

    private static final LocalDate LOGIN_DATE = LocalDate.of(2026, 1, 10);

    @Test
    void onMessage_processesLoginForValidEligibleUser() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(true);

        consumer.onMessage(new UserLoggedInEvent(1L, LOGIN_DATE, 0L));

        verify(missionProgressService).processLogin(1L, LOGIN_DATE);
    }

    @Test
    void onMessage_skipsWhenUserDoesNotExist() {
        when(userQueryPort.userExists(99L)).thenReturn(false);

        consumer.onMessage(new UserLoggedInEvent(99L, LOGIN_DATE, 0L));

        verify(missionProgressService, never()).processLogin(any(), any());
    }

    @Test
    void onMessage_skipsWhenUserNotEligible() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(false);

        consumer.onMessage(new UserLoggedInEvent(1L, LOGIN_DATE, 0L));

        verify(missionProgressService, never()).processLogin(any(), any());
    }
}