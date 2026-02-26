package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.application.port.GameLaunchRecordPort;
import com.example.demo.context.mission.application.port.GamePlayRecordPort;
import com.example.demo.context.mission.application.port.LoginRecordPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMissionsQueryHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Long USER_ID = 1L;
    private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Mock UserQueryPort userQueryPort;
    @Mock MissionRepository missionRepository;
    @Mock LoginRecordPort loginRecordPort;
    @Mock GameLaunchRecordPort gameLaunchRecordPort;
    @Mock GamePlayRecordPort gamePlayRecordPort;

    GetMissionsQueryHandler handler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZONE);
        List<CriteriaComputer> computers = List.of(
            new ConsecutiveLoginCriteriaComputer(clock, loginRecordPort),
            new DifferentGamesCriteriaComputer(gameLaunchRecordPort),
            new PlayScoreCriteriaComputer(gamePlayRecordPort)
        );
        handler = new GetMissionsQueryHandler(userQueryPort, missionRepository, computers);
    }

    @Test
    void handle_throwsWhenUserNotFound() {
        when(userQueryPort.userExists(999L)).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new GetMissionsQuery(999L)))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void handle_returnsMissionResponsesWithComputedCriteria() {
        when(userQueryPort.userExists(USER_ID)).thenReturn(true);
        var missions = List.of(
            Mission.reconstitute(1L, USER_ID, MissionType.CONSECUTIVE_LOGIN, false, null, EXPIRED_AT),
            Mission.reconstitute(2L, USER_ID, MissionType.DIFFERENT_GAMES, true,
                LocalDateTime.of(2026, 1, 20, 10, 0), EXPIRED_AT),
            Mission.reconstitute(3L, USER_ID, MissionType.PLAY_SCORE, false, null, EXPIRED_AT)
        );
        when(missionRepository.findByUserId(USER_ID)).thenReturn(missions);
        when(loginRecordPort.countConsecutiveLoginDays(USER_ID, java.time.LocalDate.now(Clock.fixed(NOW, ZONE)))).thenReturn(2);
        when(gameLaunchRecordPort.countDistinctGamesLaunched(USER_ID)).thenReturn(3);
        when(gamePlayRecordPort.countPlaySessions(USER_ID)).thenReturn(5);
        when(gamePlayRecordPort.sumPlayScores(USER_ID)).thenReturn(800);

        List<MissionResponse> result = handler.handle(new GetMissionsQuery(USER_ID));

        assertThat(result).hasSize(3);

        // CONSECUTIVE_LOGIN: single criterion
        assertThat(result.get(0).missionType()).isEqualTo(MissionType.CONSECUTIVE_LOGIN);
        assertThat(result.get(0).criteria()).hasSize(1);
        assertThat(result.get(0).criteria().get(0).label()).isEqualTo("consecutiveDays");
        assertThat(result.get(0).criteria().get(0).progress()).isEqualTo(2);
        assertThat(result.get(0).criteria().get(0).target()).isEqualTo(3);
        assertThat(result.get(0).completed()).isFalse();
        assertThat(result.get(0).description()).isEqualTo(MissionType.CONSECUTIVE_LOGIN.getDescription());

        // DIFFERENT_GAMES: completed
        assertThat(result.get(1).missionType()).isEqualTo(MissionType.DIFFERENT_GAMES);
        assertThat(result.get(1).completed()).isTrue();
        assertThat(result.get(1).completedAt()).isEqualTo(LocalDateTime.of(2026, 1, 20, 10, 0));

        // PLAY_SCORE: two criteria
        assertThat(result.get(2).criteria()).hasSize(2);
        assertThat(result.get(2).criteria().get(0).label()).isEqualTo("sessions");
        assertThat(result.get(2).criteria().get(0).progress()).isEqualTo(5);
        assertThat(result.get(2).criteria().get(1).label()).isEqualTo("totalScore");
        assertThat(result.get(2).criteria().get(1).progress()).isEqualTo(800);
    }

    @Test
    void handle_returnsEmptyListWhenNoMissionsExist() {
        when(userQueryPort.userExists(USER_ID)).thenReturn(true);
        when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<MissionResponse> result = handler.handle(new GetMissionsQuery(USER_ID));

        assertThat(result).isEmpty();
    }
}