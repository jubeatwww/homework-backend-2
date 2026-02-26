package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.application.port.GameLaunchRecordPort;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse.Criterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DifferentGamesCriteriaComputer implements CriteriaComputer {

    private final GameLaunchRecordPort gameLaunchRecordPort;

    @Override
    public MissionType supportedType() {
        return MissionType.DIFFERENT_GAMES;
    }

    @Override
    public List<Criterion> compute(Long userId) {
        int games = gameLaunchRecordPort.countDistinctGamesLaunched(userId);
        return List.of(new Criterion("distinctGames", games, 3));
    }
}