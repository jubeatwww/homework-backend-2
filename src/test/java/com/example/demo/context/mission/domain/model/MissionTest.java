package com.example.demo.context.mission.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MissionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime FUTURE = NOW.plusDays(30);
    private static final LocalDateTime PAST = NOW.minusDays(1);

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_setsAllFieldsCorrectly() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        assertThat(mission.getId()).isNull();
        assertThat(mission.getUserId()).isEqualTo(1L);
        assertThat(mission.getMissionType()).isEqualTo(MissionType.CONSECUTIVE_LOGIN);
        assertThat(mission.getProgress()).isZero();
        assertThat(mission.getTarget()).isEqualTo(MissionType.CONSECUTIVE_LOGIN.getTarget());
        assertThat(mission.isCompleted()).isFalse();
        assertThat(mission.getCompletedAt()).isNull();
        assertThat(mission.getExpiredAt()).isEqualTo(FUTURE);
        assertThat(mission.getVersion()).isZero();
    }

    // ── reconstitute ────────────────────────────────────────────────────────

    @Test
    void reconstitute_setsAllFieldsCorrectly() {
        Mission mission = Mission.reconstitute(
            10L, 1L, MissionType.DIFFERENT_GAMES,
            2, 3, false, null, FUTURE, 1
        );

        assertThat(mission.getId()).isEqualTo(10L);
        assertThat(mission.getUserId()).isEqualTo(1L);
        assertThat(mission.getMissionType()).isEqualTo(MissionType.DIFFERENT_GAMES);
        assertThat(mission.getProgress()).isEqualTo(2);
        assertThat(mission.getTarget()).isEqualTo(3);
        assertThat(mission.isCompleted()).isFalse();
        assertThat(mission.getCompletedAt()).isNull();
        assertThat(mission.getExpiredAt()).isEqualTo(FUTURE);
        assertThat(mission.getVersion()).isEqualTo(1);
    }

    // ── isExpired ───────────────────────────────────────────────────────────

    @Test
    void isExpired_returnsTrueWhenNowIsAfterExpiry() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, PAST);

        assertThat(mission.isExpired(NOW)).isTrue();
    }

    @Test
    void isExpired_returnsFalseWhenNowIsBeforeExpiry() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        assertThat(mission.isExpired(NOW)).isFalse();
    }

    @Test
    void isExpired_returnsFalseWhenExpiryIsNull() {
        Mission mission = Mission.reconstitute(1L, 1L, MissionType.CONSECUTIVE_LOGIN, 0, 3, false, null, null, 0);

        assertThat(mission.isExpired(NOW)).isFalse();
    }

    // ── advanceProgress ─────────────────────────────────────────────────────

    @Test
    void advanceProgress_updatesProgressWhenHigher() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        mission.advanceProgress(2, NOW);

        assertThat(mission.getProgress()).isEqualTo(2);
        assertThat(mission.isCompleted()).isFalse();
    }

    @Test
    void advanceProgress_completesWhenProgressReachesTarget() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        mission.advanceProgress(3, NOW); // target is 3

        assertThat(mission.getProgress()).isEqualTo(3);
        assertThat(mission.isCompleted()).isTrue();
        assertThat(mission.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void advanceProgress_doesNotDecreaseProgress() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);
        mission.advanceProgress(2, NOW);

        mission.advanceProgress(1, NOW);

        assertThat(mission.getProgress()).isEqualTo(2);
    }

    @Test
    void advanceProgress_doesNothingWhenAlreadyCompleted() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);
        mission.advanceProgress(3, NOW);

        LocalDateTime later = NOW.plusHours(1);
        mission.advanceProgress(3, later);

        assertThat(mission.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void advanceProgress_doesNothingWhenExpired() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, PAST);

        mission.advanceProgress(3, NOW);

        assertThat(mission.getProgress()).isZero();
        assertThat(mission.isCompleted()).isFalse();
    }

    @Test
    void advanceProgress_withCanCompleteFalse_doesNotCompleteEvenAtTarget() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        mission.advanceProgress(3, false, NOW);

        assertThat(mission.getProgress()).isEqualTo(3);
        assertThat(mission.isCompleted()).isFalse();
        assertThat(mission.getCompletedAt()).isNull();
    }

    @Test
    void advanceProgress_withCanCompleteFalse_canStillBeCompletedLater() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);
        mission.advanceProgress(3, false, NOW);

        mission.advanceProgress(3, true, NOW.plusMinutes(1));

        assertThat(mission.isCompleted()).isTrue();
    }
}