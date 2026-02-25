package com.example.demo.context.mission.domain.repository;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MissionRepository {

    List<Mission> findByUserId(Long userId);

    Optional<Mission> findByUserIdAndMissionType(Long userId, MissionType missionType);

    /**
     * Atomically marks a mission as completed.
     * Only transitions from completed=false to completed=true (single-direction latch).
     *
     * @return true if transition happened, false if already completed or not found
     */
    boolean completeMission(Long userId, MissionType missionType, LocalDateTime completedAt);

    void createAllIfAbsent(List<Mission> missions);
}