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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock TransactionTemplate transactionTemplate;
    @Mock DistributedLock distributedLock;

    MissionProgressService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZONE);
        service = new MissionProgressService(
            clock, missionRepository, loginRecordPort, gameLaunchRecordPort, gamePlayRecordPort,
            missionCompletionCache, rewardEventPublisher, rewardRepository, transactionTemplate, distributedLock
        );

        // TransactionTemplate: execute the callback directly
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            var callback = invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        // DistributedLock: always acquire lock and run action
        lenient().when(distributedLock.tryWithLock(anyString(), anyLong(), any(), any())).thenAnswer(invocation -> {
            var action = invocation.getArgument(2, java.util.function.Supplier.class);
            return action.get();
        });

        // Default: save returns the mission as-is
        lenient().when(missionRepository.save(any(Mission.class))).thenAnswer(returnsFirstArg());
    }

    // ── processLogin ────────────────────────────────────────────────────────

    @Nested
    class ProcessLogin {

        @Test
        void skipsProgressUpdateWhenCacheMarkedCompleted() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(true);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(loginRecordPort, never()).recordLogin(any(), any());
        }

        @Test
        void skipsProgressWhenDuplicateRecord() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(false);
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(transactionTemplate, never()).execute(any());
        }

        @Test
        void advancesProgressWhenNewRecord() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(2);

            Mission mission = Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Optional.of(mission));

            // Not all completed → skip reward grant
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(mission));

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(missionRepository).save(any(Mission.class));
        }

        @Test
        void marksCacheCompletedWhenMissionCompletes() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3); // target

            Mission mission = Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Optional.of(mission));
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(mission));

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(missionCompletionCache).markCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN);
        }
    }

    // ── processGameLaunch ───────────────────────────────────────────────────

    @Nested
    class ProcessGameLaunch {

        @Test
        void advancesProgressWhenNewGameLaunched() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.DIFFERENT_GAMES)).thenReturn(false);
            when(gameLaunchRecordPort.recordGameLaunch(USER_ID, GAME_ID)).thenReturn(true);
            when(gameLaunchRecordPort.countDistinctGamesLaunched(USER_ID)).thenReturn(2);

            Mission mission = Mission.create(USER_ID, MissionType.DIFFERENT_GAMES,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.DIFFERENT_GAMES))
                .thenReturn(Optional.of(mission));
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(mission));

            service.processGameLaunch(USER_ID, GAME_ID);

            verify(missionRepository).save(any(Mission.class));
        }
    }

    // ── processGamePlay ─────────────────────────────────────────────────────

    @Nested
    class ProcessGamePlay {

        @Test
        void advancesProgressWhenNewGamePlayed() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 500, "key-1")).thenReturn(true);
            when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(500);
            // countPlaySessions not called because 500 < 1000 target (short-circuit &&)

            Mission mission = Mission.create(USER_ID, MissionType.PLAY_SCORE,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.PLAY_SCORE))
                .thenReturn(Optional.of(mission));
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(mission));

            service.processGamePlay(USER_ID, GAME_ID, 500, "key-1");

            verify(missionRepository).save(any(Mission.class));
        }

        @Test
        void doesNotCompleteWhenSessionCountBelowThree() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.PLAY_SCORE)).thenReturn(false);
            when(gamePlayRecordPort.recordGamePlay(USER_ID, GAME_ID, 1200, "key-1")).thenReturn(true);
            when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(1200); // above 1000 target
            when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(2); // below 3 sessions guard

            Mission mission = Mission.create(USER_ID, MissionType.PLAY_SCORE,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.PLAY_SCORE))
                .thenReturn(Optional.of(mission));
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(mission));

            service.processGamePlay(USER_ID, GAME_ID, 1200, "key-1");

            // Progress updated but not completed (canComplete = false due to guard)
            verify(missionRepository).save(any(Mission.class));
            verify(missionCompletionCache, never()).markCompleted(USER_ID, MissionType.PLAY_SCORE);
        }
    }

    // ── optimistic locking retry ────────────────────────────────────────────

    @Nested
    class OptimisticLockingRetry {

        @Test
        void retriesOnOptimisticLockFailure() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(2);

            // Return a FRESH mission each call so retry sees unmodified state
            LocalDateTime expiredAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30);
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenAnswer(inv -> Optional.of(Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN, expiredAt)));
            when(missionRepository.save(any(Mission.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"))
                .thenAnswer(returnsFirstArg());
            when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of(
                Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN, expiredAt)));

            service.processLogin(USER_ID, LOGIN_DATE);

            // save called twice: first fails, second succeeds
            verify(missionRepository, times(2)).save(any(Mission.class));
        }

        @Test
        void throwsAfterMaxRetries() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(2);

            // Return a FRESH mission each call so retry sees unmodified state
            LocalDateTime expiredAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30);
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenAnswer(inv -> Optional.of(Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN, expiredAt)));
            when(missionRepository.save(any(Mission.class)))
                .thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> service.processLogin(USER_ID, LOGIN_DATE))
                .isInstanceOf(OptimisticLockingFailureException.class);

            verify(missionRepository, times(3)).save(any(Mission.class));
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

            Mission completedMission = createCompletedMission(MissionType.CONSECUTIVE_LOGIN);
            Mission completedMission2 = createCompletedMission(MissionType.DIFFERENT_GAMES);
            Mission completedMission3 = createCompletedMission(MissionType.PLAY_SCORE);

            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Optional.of(Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN,
                    LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30))));
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(false);
            when(missionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(completedMission, completedMission2, completedMission3));
            when(rewardRepository.grantReward(USER_ID, 777)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(rewardEventPublisher).publish(any());
        }

        @Test
        void skipsRewardWhenAlreadyGranted() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(3);

            Mission completedMission = createCompletedMission(MissionType.CONSECUTIVE_LOGIN);
            Mission completedMission2 = createCompletedMission(MissionType.DIFFERENT_GAMES);
            Mission completedMission3 = createCompletedMission(MissionType.PLAY_SCORE);

            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Optional.of(Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN,
                    LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30))));
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(false);
            when(missionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(completedMission, completedMission2, completedMission3));
            when(rewardRepository.grantReward(USER_ID, 777)).thenReturn(false);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(rewardEventPublisher, never()).publish(any());
            verify(missionCompletionCache).markAllCompleted(USER_ID);
        }

        @Test
        void skipsRewardCheckWhenAllCompletedCacheHit() {
            when(missionCompletionCache.isCompleted(USER_ID, MissionType.CONSECUTIVE_LOGIN)).thenReturn(false);
            when(loginRecordPort.recordLogin(USER_ID, LOGIN_DATE)).thenReturn(true);
            when(loginRecordPort.countConsecutiveLoginDays(USER_ID, LOGIN_DATE)).thenReturn(2);

            Mission mission = Mission.create(USER_ID, MissionType.CONSECUTIVE_LOGIN,
                LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30));
            when(missionRepository.findByUserIdAndMissionType(USER_ID, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Optional.of(mission));
            when(missionCompletionCache.isAllCompleted(USER_ID)).thenReturn(true);

            service.processLogin(USER_ID, LOGIN_DATE);

            verify(distributedLock, never()).tryWithLock(any(), anyLong(), any(), any());
        }
    }

    private Mission createCompletedMission(MissionType type) {
        LocalDateTime expiredAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).plusDays(30);
        return Mission.reconstitute(
            (long) type.ordinal() + 1, USER_ID, type,
            type.getTarget(), type.getTarget(), true,
            LocalDateTime.now(Clock.fixed(NOW, ZONE)), expiredAt, 1
        );
    }
}