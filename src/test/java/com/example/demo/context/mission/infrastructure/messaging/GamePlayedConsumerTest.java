package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.GamePlayedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamePlayedConsumerTest {

    @Mock UserQueryPort userQueryPort;
    @Mock GameQueryPort gameQueryPort;
    @Mock UserEligibilityService userEligibilityService;
    @Mock MissionProgressService missionProgressService;
    @InjectMocks GamePlayedConsumer consumer;

    @Test
    void onMessage_processesGamePlayForValidEligibleUser() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(10L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(true);

        consumer.onMessage(new GamePlayedEvent(1L, 10L, 500, "key-1", 0L));

        verify(missionProgressService).processGamePlay(1L, 10L, 500, "key-1");
    }

    @Test
    void onMessage_skipsWhenIdempotencyKeyIsNull() {
        consumer.onMessage(new GamePlayedEvent(1L, 10L, 500, null, 0L));

        verify(missionProgressService, never()).processGamePlay(any(), any(), anyInt(), any());
    }

    @Test
    void onMessage_skipsWhenIdempotencyKeyIsBlank() {
        consumer.onMessage(new GamePlayedEvent(1L, 10L, 500, "  ", 0L));

        verify(missionProgressService, never()).processGamePlay(any(), any(), anyInt(), any());
    }

    @Test
    void onMessage_skipsWhenUserDoesNotExist() {
        when(userQueryPort.userExists(99L)).thenReturn(false);

        consumer.onMessage(new GamePlayedEvent(99L, 10L, 500, "key-1", 0L));

        verify(missionProgressService, never()).processGamePlay(any(), any(), anyInt(), any());
    }

    @Test
    void onMessage_skipsWhenGameDoesNotExist() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(99L)).thenReturn(false);

        consumer.onMessage(new GamePlayedEvent(1L, 99L, 500, "key-1", 0L));

        verify(missionProgressService, never()).processGamePlay(any(), any(), anyInt(), any());
    }

    @Test
    void onMessage_skipsWhenUserNotEligible() {
        when(userQueryPort.userExists(1L)).thenReturn(true);
        when(gameQueryPort.gameExists(10L)).thenReturn(true);
        when(userEligibilityService.isEligible(1L)).thenReturn(false);

        consumer.onMessage(new GamePlayedEvent(1L, 10L, 500, "key-1", 0L));

        verify(missionProgressService, never()).processGamePlay(any(), any(), anyInt(), any());
    }
}