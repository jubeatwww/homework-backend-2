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
        assertThat(mission.isCompleted()).isFalse();
        assertThat(mission.getCompletedAt()).isNull();
        assertThat(mission.getExpiredAt()).isEqualTo(FUTURE);
    }

    // ── reconstitute ────────────────────────────────────────────────────────

    @Test
    void reconstitute_setsAllFieldsCorrectly() {
        Mission mission = Mission.reconstitute(
            10L, 1L, MissionType.DIFFERENT_GAMES,
            false, null, FUTURE
        );

        assertThat(mission.getId()).isEqualTo(10L);
        assertThat(mission.getUserId()).isEqualTo(1L);
        assertThat(mission.getMissionType()).isEqualTo(MissionType.DIFFERENT_GAMES);
        assertThat(mission.isCompleted()).isFalse();
        assertThat(mission.getCompletedAt()).isNull();
        assertThat(mission.getExpiredAt()).isEqualTo(FUTURE);
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
        Mission mission = Mission.reconstitute(1L, 1L, MissionType.CONSECUTIVE_LOGIN, false, null, null);

        assertThat(mission.isExpired(NOW)).isFalse();
    }

    // ── complete ─────────────────────────────────────────────────────────────

    @Test
    void complete_transitionsToCompletedAndSetsTimestamp() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);

        boolean result = mission.complete(NOW);

        assertThat(result).isTrue();
        assertThat(mission.isCompleted()).isTrue();
        assertThat(mission.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void complete_returnsFalseWhenAlreadyCompleted() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, FUTURE);
        mission.complete(NOW);

        boolean result = mission.complete(NOW.plusHours(1));

        assertThat(result).isFalse();
        assertThat(mission.getCompletedAt()).isEqualTo(NOW); // original timestamp preserved
    }

    @Test
    void complete_returnsFalseWhenExpired() {
        Mission mission = Mission.create(1L, MissionType.CONSECUTIVE_LOGIN, PAST);

        boolean result = mission.complete(NOW);

        assertThat(result).isFalse();
        assertThat(mission.isCompleted()).isFalse();
    }
}