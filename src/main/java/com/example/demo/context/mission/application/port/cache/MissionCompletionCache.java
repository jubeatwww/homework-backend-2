package com.example.demo.context.mission.application.port.cache;

import com.example.demo.context.mission.domain.model.MissionType;

public interface MissionCompletionCache {

    boolean isCompleted(Long userId, MissionType missionType);

    void markCompleted(Long userId, MissionType missionType);

    boolean isAllCompleted(Long userId);

    void markAllCompleted(Long userId);
}
