package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.GameLaunchedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameLaunchedConsumerTest {

    @Mock UserQueryPort userQueryPort;
    @Mock GameQueryPort gameQueryPort;
    @Mock UserEligibilityService userEligibilityService;
    @Mock MissionProgressService missionProgressService;
    @InjectMocks GameLaunchedConsumer consumer;

    @Test
    void onMessage_processesGameLaunchForValidEligibleUser() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(10L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(true);

        consumer.onMessage(new GameLaunchedEvent(1L, 10L, 0L));

        verify(missionProgressService).processGameLaunch(1L, 10L);
    }

    @Test
    void onMessage_skipsWhenUserDoesNotExist() {
        when(userQueryPort.userExists(99L)).thenReturn(false);

        consumer.onMessage(new GameLaunchedEvent(99L, 10L, 0L));

        verify(missionProgressService, never()).processGameLaunch(any(), any());
    }

    @Test
    void onMessage_skipsWhenGameDoesNotExist() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(99L)).thenReturn(false);

        consumer.onMessage(new GameLaunchedEvent(1L, 99L, 0L));

        verify(missionProgressService, never()).processGameLaunch(any(), any());
    }

    @Test
    void onMessage_skipsWhenUserNotEligible() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(10L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(false);

        consumer.onMessage(new GameLaunchedEvent(1L, 10L, 0L));

        verify(missionProgressService, never()).processGameLaunch(any(), any());
    }
}