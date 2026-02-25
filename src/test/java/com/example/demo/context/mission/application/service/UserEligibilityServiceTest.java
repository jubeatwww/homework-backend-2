package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.port.cache.UserEligibilityCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEligibilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    UserEligibilityCache userEligibilityCache;

    @Mock
    UserQueryPort userQueryPort;

    @InjectMocks
    UserEligibilityService service;

    // We cannot use @InjectMocks for Clock (it's not a mock), so we build manually.
    private UserEligibilityService serviceWithClock(Clock clock) {
        return new UserEligibilityService(clock, userEligibilityCache, userQueryPort);
    }

    // ── cache hit: expired ──────────────────────────────────────────────────

    @Test
    void isEligible_returnsFalseWhenCacheMarkedExpired() {
        when(userEligibilityCache.isExpired(1L)).thenReturn(true);
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isFalse();
        verify(userQueryPort, never()).getUserCreatedAt(any());
    }

    // ── cache miss: DB says eligible (within 30 days) ───────────────────────

    @Test
    void isEligible_returnsTrueWhenUserCreatedWithin30Days() {
        when(userEligibilityCache.isExpired(1L)).thenReturn(false);
        LocalDateTime createdAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).minusDays(10);
        when(userQueryPort.getUserCreatedAt(1L)).thenReturn(Optional.of(createdAt));
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isTrue();
    }

    // ── cache miss: DB says expired (>30 days), marks cache ─────────────────

    @Test
    void isEligible_returnsFalseWhenUserCreatedMoreThan30DaysAgo() {
        when(userEligibilityCache.isExpired(1L)).thenReturn(false);
        LocalDateTime createdAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).minusDays(31);
        when(userQueryPort.getUserCreatedAt(1L)).thenReturn(Optional.of(createdAt));
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isFalse();
        verify(userEligibilityCache).markExpired(1L);
    }

    // ── user not found in DB → eligible (fail-open) ─────────────────────────

    @Test
    void isEligible_returnsTrueWhenUserNotFoundInDb() {
        when(userEligibilityCache.isExpired(1L)).thenReturn(false);
        when(userQueryPort.getUserCreatedAt(1L)).thenReturn(Optional.empty());
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isTrue();
    }

    // ── cache exception on check → falls through to DB ──────────────────────

    @Test
    void isEligible_fallsBackToDbWhenCacheCheckFails() {
        when(userEligibilityCache.isExpired(1L)).thenThrow(new RuntimeException("Redis down"));
        LocalDateTime createdAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).minusDays(5);
        when(userQueryPort.getUserCreatedAt(1L)).thenReturn(Optional.of(createdAt));
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isTrue();
    }

    // ── cache exception on markExpired → still returns false ────────────────

    @Test
    void isEligible_returnsFalseEvenWhenCacheMarkFails() {
        when(userEligibilityCache.isExpired(1L)).thenReturn(false);
        LocalDateTime createdAt = LocalDateTime.now(Clock.fixed(NOW, ZONE)).minusDays(31);
        when(userQueryPort.getUserCreatedAt(1L)).thenReturn(Optional.of(createdAt));
        doThrow(new RuntimeException("Redis down")).when(userEligibilityCache).markExpired(1L);
        var svc = serviceWithClock(Clock.fixed(NOW, ZONE));

        assertThat(svc.isEligible(1L)).isFalse();
    }
}