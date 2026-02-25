package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.*;
import com.example.demo.context.mission.application.port.cache.MissionCompletionCache;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionProgressServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Long USER_ID = 1L;
    private static final Long GAME_ID = 10L;
    private static final LocalDate LOGIN_DATE = LocalDate.of(2026, 2, 1);

    @Mock MissionRepository missionRepository;
    @Mock LoginRecordPort loginRecordPort;
    @Mock GameLaunchRecordPort gameLaunchRecordPort;
    @Mock GamePlayRecordPort gamePlayRecordPort;
    @Mock MissionCompletionCache missionCompletionCache;
    @Mock RewardEventPublisher rewardEventPublisher;
    @Mock RewardRepository rewardRepository;
    @Mock DistributedLock distributedLock;

    MissionProgressService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZONE);
        service = new MissionProgressService(
            clock, missionRepository, loginRecordPort, gameLaunchRecordPort, gamePlayRecordPort,
            missionCompletionCache, rewardEventPublisher, rewardRepository, distributedLock
        );

        // DistributedLock: always acquire lock and run action
        lenient().when(distributedLock.tryWithLock(anyString(), anyLong(), any(), any())).thenAnswer(invocation -> {
            var action = invocation.getArgument(2, java.util.function.Supplier.class);
            return action.get();
        });
    }

    // ── processLogin ────────────────────────────────────────────────────────

    @Nested
    class ProcessLogin {

        @Test
        void skipsRecordAndCheckWhenCacheMarkedCompleted() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(loginRecordPort, never()).recordLogin(any(), any());
            verify(missionRepository, never()).completeMission(any(), any(), any());
        }

        @Test
        void recordsLoginAndCompletesWhenTargetReached() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.CONSECUTIVE_LOGIN), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(missionRepository).completeMission(eq(USER_ID), eq(MissionType.CONSECUTIVE_LOGIN), any());
            verify(missionCompletionCache).markCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN);
        }

        @Test
        void recordsLoginButDoesNotCompleteWhenTargetNotReached() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(2);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(missionRepository, never()).completeMission(any(), any(), any());
        }
    }

    // ── processGameLaunch ───────────────────────────────────────────────────

    @Nested
    class ProcessGameLaunch {

        @Test
        void completesWhenThreeDistinctGames() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.DIFFERENT_GAMES)).thenReturn(false);
            when(gameLaunchRecordPort.recordGameLaunch(USER_ID, GAME_ID)).thenReturn(true);
            when(gameLaunchRecordPort.countDistinctGamesLaunched(USER_ID)).thenReturn(3);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.DIFFERENT_GAMES), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processGameLaunch(USER_ID, GAME_ID);

            verify(missionRepository).completeMission(eq(USER_ID), eq(MissionType.DIFFERENT_GAMES), any());
        }

        @Test
        void doesNotCompleteWhenOnlyTwoGames() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.DIFFERENT_GAMES)).thenReturn(false);
            when(gameLaunchRecordPort.recordGameLaunch(USER_ID, GAME_ID)).thenReturn(true);
            when(gameLaunchRecordPort.countDistinctGamesLaunched(USER_ID)).thenReturn(2);

            service.processGameLaunch(USER_ID, GAME_ID);

            verify(missionRepository, never()).completeMission(any(), any(), any());
        }
    }

    // ── processGamePlay ─────────────────────────────────────────────────────

    @Nested
    class ProcessGamePlay {

        @Test
        void completesWhenBothSessionAndScoreTargetsMet() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 500, "key-1")).thenReturn(true);
            when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(3);
            when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(1200);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.PLAY_SCORE), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processGamePlay(USER_ID, GAME_ID, 500, "key-1");

            verify(missionRepository).completeMission(eq(USER_ID), eq(MissionType.PLAY_SCORE), any());
        }

        @Test
        void doesNotCompleteWhenSessionCountBelowThree() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 1200, "key-1")).thenReturn(true);
            when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(2);

            service.processGamePlay(USER_ID, GAME_ID, 1200, "key-1");

            verify(missionRepository, never()).completeMission(any(), any(), any());
        }

        @Test
        void doesNotCompleteWhenScoreIsExactlyOneThousand() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 1000, "key-1000")).thenReturn(true);
            when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(3);
            when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(1000);

            service.processGamePlay(USER_ID, GAME_ID, 1000, "key-1000");

            verify(missionRepository, never()).completeMission(any(), any(), any());
        }

        @Test
        void doesNotCompleteWhenScoreBelowThreshold() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 200, "key-1")).thenReturn(true);
            when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(3);
            when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(600);

            service.processGamePlay(USER_ID, GAME_ID, 200, "key-1");

            verify(missionRepository, never()).completeMission(any(), any(), any());
        }
    }

    // ── tryGrantReward ──────────────────────────────────────────────────────

    @Nested
    class TryGrantReward {

        @Test
        void grantsRewardWhenAllMissionsCompleted() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.CONSECUTIVE_LOGIN), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(false);
            when(missionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(
                    createCompletedMission(MissionType.CONSECUTIVE_LOGIN),
                    createCompletedMission(MissionType.DIFFERENT_GAMES),
                    createCompletedMission(MissionType.PLAY_SCORE)
                ));
            when(rewardRepository.grantReward(USER_ID, 777)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(rewardEventPublisher).publish(any());
        }

        @Test
        void skipsRewardWhenAlreadyGranted() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.CONSECUTIVE_LOGIN), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(false);
            when(missionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(
                    createCompletedMission(MissionType.CONSECUTIVE_LOGIN),
                    createCompletedMission(MissionType.DIFFERENT_GAMES),
                    createCompletedMission(MissionType.PLAY_SCORE)
                ));
            when(rewardRepository.grantReward(USER_ID, 777)).thenReturn(false);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(rewardEventPublisher, never()).publish(any());
            verify(missionCompletionCache).markAllCompleted(USER_ID);
        }

        @Test
        void skipsRewardCheckWhenAllCompletedCacheHit() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3);
            when(missionRepository.completeMission(eq(USER_ID), eq(MissionType.CONSECUTIVE_LOGIN), any()))
                .thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(distributedLock, never()).tryWithLock(any(), anyLong(), any(), any());
        }
    }

    private Mission createCompletedMission(MissionType type) {
        LocalDateTime expiredAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30);
        return Mission.reconstitute(
            (long) type.ordinal() + 1, USER_ID, type,
            true, LocalDateTime.now(Clock.fixed(NOW, ZONE)), expiredAt
        );
    }
}
