package com.example.demo.context.mission.domain.repository;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;

import java.util.List;
import java.util.Optional;

public interface MissionRepository {

    List<Mission> findByUserId(Long userId);

    Optional<Mission> findByUserIdAndMissionType(Long userId, MissionType missionType);

    Mission save(Mission mission);

    void createAllIfAbsent(List<Mission> missions);
}