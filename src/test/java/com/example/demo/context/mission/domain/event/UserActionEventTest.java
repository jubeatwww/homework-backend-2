package com.example.demo.context.mission.domain.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserActionEventTest {

    // ── eventName ───────────────────────────────────────────────────────────

    @Test
    void userLoggedInEvent_eventName_isKebabCase() {
        var event = new UserLoggedInEvent(1L, LocalDate.of(2026, 1, 1));

        assertThat(event.eventName()).isEqualTo("user-logged-in");
    }

    @Test
    void gameLaunchedEvent_eventName_isKebabCase() {
        var event = new GameLaunchedEvent(1L, 10L);

        assertThat(event.eventName()).isEqualTo("game-launched");
    }

    @Test
    void gamePlayedEvent_eventName_isKebabCase() {
        var event = new GamePlayedEvent(1L, 10L, 100, "key");

        assertThat(event.eventName()).isEqualTo("game-played");
    }

    // ── dedupKey ────────────────────────────────────────────────────────────

    @Test
    void userLoggedInEvent_dedupKey_containsUserIdAndDate() {
        var event = new UserLoggedInEvent(1L, LocalDate.of(2026, 1, 15));

        assertThat(event.dedupKey()).isEqualTo("1:2026-01-15");
    }

    @Test
    void gameLaunchedEvent_dedupKey_containsUserIdAndGameId() {
        var event = new GameLaunchedEvent(1L, 10L);

        assertThat(event.dedupKey()).isEqualTo("1:10");
    }

    @Test
    void gamePlayedEvent_dedupKey_containsUserIdAndIdempotencyKey() {
        var event = new GamePlayedEvent(1L, 10L, 100, "idem-abc");

        assertThat(event.dedupKey()).isEqualTo("1:idem-abc");
    }

    // ── eventKey ────────────────────────────────────────────────────────────

    @Test
    void eventKey_combinesEventNameAndDedupKey() {
        var event = new UserLoggedInEvent(1L, LocalDate.of(2026, 1, 15));

        assertThat(event.eventKey()).isEqualTo("user-logged-in:1:2026-01-15");
    }

    // ── eventName caching (same result on repeated calls) ───────────────────

    @Test
    void eventName_returnsSameValueOnRepeatedCalls() {
        var event = new GameLaunchedEvent(2L, 5L);

        assertThat(event.eventName()).isEqualTo(event.eventName());
    }
}