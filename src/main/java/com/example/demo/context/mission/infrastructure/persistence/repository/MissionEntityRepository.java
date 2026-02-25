package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.infrastructure.persistence.entity.MissionEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MissionEntityRepository extends CrudRepository<MissionEntity, Long> {

    List<MissionEntity> findByUserId(Long userId);

    Optional<MissionEntity> findByUserIdAndMissionType(Long userId, MissionType missionType);
}
