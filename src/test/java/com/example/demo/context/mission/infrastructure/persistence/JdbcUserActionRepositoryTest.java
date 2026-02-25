package com.example.demo.context.mission.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcUserActionRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    JdbcUserActionRepository repository;

    // ── countConsecutiveLoginDays ────────────────────────────────────────────

    @Test
    void countConsecutiveLoginDays_returnsFullStreakWhenAllDaysConsecutive() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 3)
        );
        mockLoginDates(1L, asOfDate, dates);

        assertThat(repository.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(3);
    }

    @Test
    void countConsecutiveLoginDays_stopsAtFirstGap() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        // Jan 4 is missing → streak breaks after Jan 5
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 3)
        );
        mockLoginDates(1L, asOfDate, dates);

        assertThat(repository.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(1);
    }

    @Test
    void countConsecutiveLoginDays_returnsZeroWhenNoLogins() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        mockLoginDates(1L, asOfDate, List.of());

        assertThat(repository.countConsecutiveLoginDays(1L, asOfDate)).isZero();
    }

    @Test
    void countConsecutiveLoginDays_returnsZeroWhenLatestLoginIsNotAsOfDate() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        // Most recent login is Jan 4 — streak doesn't include asOfDate
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 3)
        );
        mockLoginDates(1L, asOfDate, dates);

        assertThat(repository.countConsecutiveLoginDays(1L, asOfDate)).isZero();
    }

    @Test
    void countConsecutiveLoginDays_returnsSingleDayStreak() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        List<LocalDate> dates = List.of(LocalDate.of(2026, 1, 5));
        mockLoginDates(1L, asOfDate, dates);

        assertThat(repository.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    private void mockLoginDates(Long userId, LocalDate asOfDate, List<LocalDate> dates) {
        when(jdbcTemplate.query(
            anyString(),
            any(RowMapper.class),
            eq(userId),
            eq(asOfDate)
        )).thenReturn(dates);
    }
}