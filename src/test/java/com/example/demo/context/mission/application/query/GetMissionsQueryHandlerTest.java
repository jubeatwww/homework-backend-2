package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMissionsQueryHandlerTest {

    @Mock
    MissionRepository missionRepository;

    @InjectMocks
    GetMissionsQueryHandler handler;

    private static final Long USER_ID = 1L;
    private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Test
    void handle_returnsMissionResponsesForUser() {
        var missions = List.of(
            Mission.reconstitute(1L, USER_ID, MissionType.CONSECUTIVE_LOGIN, 2, 3, false, null, EXPIRED_AT, 0),
            Mission.reconstitute(2L, USER_ID, MissionType.DIFFERENT_GAMES, 3, 3, true,
                LocalDateTime.of(2026, 1, 20, 10, 0), EXPIRED_AT, 1),
            Mission.reconstitute(3L, USER_ID, MissionType.PLAY_SCORE, 500, 1000, false, null, EXPIRED_AT, 0)
        );
        when(missionRepository.findByUserId(USER_ID)).thenReturn(missions);

        List<MissionResponse> result = handler.handle(new GetMissionsQuery(USER_ID));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).missionType()).isEqualTo(MissionType.CONSECUTIVE_LOGIN);
        assertThat(result.get(0).progress()).isEqualTo(2);
        assertThat(result.get(0).target()).isEqualTo(3);
        assertThat(result.get(0).completed()).isFalse();
        assertThat(result.get(0).description()).isEqualTo(MissionType.CONSECUTIVE_LOGIN.getDescription());

        assertThat(result.get(1).missionType()).isEqualTo(MissionType.DIFFERENT_GAMES);
        assertThat(result.get(1).completed()).isTrue();
        assertThat(result.get(1).completedAt()).isEqualTo(LocalDateTime.of(2026, 1, 20, 10, 0));
    }

    @Test
    void handle_returnsEmptyListWhenNoMissionsExist() {
        when(missionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<MissionResponse> result = handler.handle(new GetMissionsQuery(USER_ID));

        assertThat(result).isEmpty();
    }
}
