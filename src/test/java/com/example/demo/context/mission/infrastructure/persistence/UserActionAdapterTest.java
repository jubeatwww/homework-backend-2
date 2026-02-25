package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.infrastructure.persistence.repository.GameLaunchRecordEntityRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.GamePlayRecordEntityRepository;
import com.example.demo.context.mission.infrastructure.persistence.repository.LoginRecordEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActionAdapterTest {

    @Mock
    LoginRecordEntityRepository loginRecordEntityRepository;

    @Mock
    GameLaunchRecordEntityRepository gameLaunchRecordEntityRepository;

    @Mock
    GamePlayRecordEntityRepository gamePlayRecordEntityRepository;

    @InjectMocks
    UserActionAdapter adapter;

    // ── countConsecutiveLoginDays ────────────────────────────────────────────

    @Test
    void countConsecutiveLoginDays_returnsFullStreakWhenAllDaysConsecutive() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 3)
        );
        when(loginRecordEntityRepository.findRecentLoginDates(1L, asOfDate)).thenReturn(dates);

        assertThat(adapter.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(3);
    }

    @Test
    void countConsecutiveLoginDays_stopsAtFirstGap() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 3)
        );
        when(loginRecordEntityRepository.findRecentLoginDates(1L, asOfDate)).thenReturn(dates);

        assertThat(adapter.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(1);
    }

    @Test
    void countConsecutiveLoginDays_returnsZeroWhenNoLogins() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        when(loginRecordEntityRepository.findRecentLoginDates(1L, asOfDate)).thenReturn(List.of());

        assertThat(adapter.countConsecutiveLoginDays(1L, asOfDate)).isZero();
    }

    @Test
    void countConsecutiveLoginDays_returnsZeroWhenLatestLoginIsNotAsOfDate() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        List<LocalDate> dates = List.of(
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 3)
        );
        when(loginRecordEntityRepository.findRecentLoginDates(1L, asOfDate)).thenReturn(dates);

        assertThat(adapter.countConsecutiveLoginDays(1L, asOfDate)).isZero();
    }

    @Test
    void countConsecutiveLoginDays_returnsSingleDayStreak() {
        LocalDate asOfDate = LocalDate.of(2026, 1, 5);
        when(loginRecordEntityRepository.findRecentLoginDates(1L, asOfDate))
            .thenReturn(List.of(LocalDate.of(2026, 1, 5)));

        assertThat(adapter.countConsecutiveLoginDays(1L, asOfDate)).isEqualTo(1);
    }
}
