package com.example.demo.context.mission.application.query;

import com.example.demo.common.cqrs.query.QueryHandler;
import com.example.demo.context.mission.application.port.GameLaunchRecordPort;
import com.example.demo.context.mission.application.port.GamePlayRecordPort;
import com.example.demo.context.mission.application.port.LoginRecordPort;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse.Criterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GetMissionsQueryHandler implements QueryHandler<GetMissionsQuery, List<MissionResponse>> {

    private final Clock clock;
    private final MissionRepository missionRepository;
    private final LoginRecordPort loginRecordPort;
    private final GameLaunchRecordPort gameLaunchRecordPort;
    private final GamePlayRecordPort gamePlayRecordPort;

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> handle(GetMissionsQuery query) {
        return missionRepository.findByUserId(query.userId()).stream()
            .map(mission -> MissionResponse.from(mission, computeCriteria(query.userId(), mission)))
            .toList();
    }

    private List<Criterion> computeCriteria(Long userId, Mission mission) {
        return switch (mission.getMissionType()) {
            case CONSECUTIVE_LOGIN -> {
                int days = loginRecordPort.countConsecutiveLoginDays(userId, LocalDate.now(clock));
                yield List.of(new Criterion("consecutiveDays", days, 3));
            }
            case DIFFERENT_GAMES -> {
                int games = gameLaunchRecordPort.countDistinctGamesLaunched(userId);
                yield List.of(new Criterion("distinctGames", games, 3));
            }
            case PLAY_SCORE -> {
                int sessions = gamePlayRecordPort.countPlaySessions(userId);
                int score = gamePlayRecordPort.sumPlayScores(userId);
                yield List.of(
                    new Criterion("sessions", sessions, 3),
                    new Criterion("totalScore", score, 1000)
                );
            }
        };
    }
}
