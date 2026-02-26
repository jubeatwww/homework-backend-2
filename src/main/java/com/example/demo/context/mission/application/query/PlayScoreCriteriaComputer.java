package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.application.port.GamePlayRecordPort;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse.Criterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlayScoreCriteriaComputer implements CriteriaComputer {

    private final GamePlayRecordPort gamePlayRecordPort;

    @Override
    public MissionType supportedType() {
        return MissionType.PLAY_SCORE;
    }

    @Override
    public List<Criterion> compute(Long userId) {
        int sessions = gamePlayRecordPort.countPlaySessions(userId);
        int score = gamePlayRecordPort.sumPlayScores(userId);
        return List.of(
            new Criterion("sessions", sessions, 3),
            new Criterion("totalScore", score, 1000)
        );
    }
}