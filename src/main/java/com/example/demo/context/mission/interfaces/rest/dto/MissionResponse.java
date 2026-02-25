package com.example.demo.context.mission.interfaces.rest.dto;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;

import java.time.LocalDateTime;

public record MissionResponse(
    Long id,
    MissionType missionType,
    String description,
    int progress,
    int target,
    boolean completed,
    LocalDateTime completedAt,
    LocalDateTime expiredAt
) {
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
            mission.getId(),
            mission.getMissionType(),
            mission.getMissionType().getDescription(),
            mission.getProgress(),
            mission.getTarget(),
            mission.isCompleted(),
            mission.getCompletedAt(),
            mission.getExpiredAt()
        );
    }
}
