package com.example.demo.context.mission.interfaces.rest.dto;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;

import java.time.LocalDateTime;
import java.util.List;

public record MissionResponse(
    Long id,
    MissionType missionType,
    String description,
    List<Criterion> criteria,
    boolean completed,
    LocalDateTime completedAt,
    LocalDateTime expiredAt
) {

    public record Criterion(String label, int progress, int target) {
    }

    public static MissionResponse from(Mission mission, List<Criterion> criteria) {
        return new MissionResponse(
            mission.getId(),
            mission.getMissionType(),
            mission.getMissionType().getDescription(),
            criteria,
            mission.isCompleted(),
            mission.getCompletedAt(),
            mission.getExpiredAt()
        );
    }
}
