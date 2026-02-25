package com.example.demo.context.mission.infrastructure.persistence;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.infrastructure.persistence.entity.MissionEntity;
import com.example.demo.context.mission.infrastructure.persistence.repository.MissionEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MissionRepositoryAdapter implements MissionRepository {

    private final MissionEntityRepository missionEntityRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Mission> findByUserId(Long userId) {
        return missionEntityRepository.findByUserId(userId).stream()
            .map(MissionEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<Mission> findByUserIdAndMissionType(Long userId, MissionType missionType) {
        return missionEntityRepository.findByUserIdAndMissionType(userId, missionType)
            .map(MissionEntity::toDomain);
    }

    @Override
    public boolean completeMission(Long userId, MissionType missionType, LocalDateTime completedAt) {
        int updated = jdbcTemplate.update(
            "UPDATE missions SET completed = true, completed_at = ? WHERE user_id = ? AND mission_type = ? AND completed = false",
            Timestamp.valueOf(completedAt), userId, missionType.name()
        );
        return updated > 0;
    }

    @Override
    public void createAllIfAbsent(List<Mission> missions) {
        if (missions.isEmpty()) {
            return;
        }
        String valuePlaceholders = String.join(", ", missions.stream()
            .map(m -> "(?, ?, ?, ?, ?)")
            .toList());
        String sql = "INSERT IGNORE INTO missions (user_id, mission_type, completed, completed_at, expired_at) VALUES "
            + valuePlaceholders;

        Object[] params = missions.stream()
            .flatMap(m -> java.util.stream.Stream.of(
                m.getUserId(),
                m.getMissionType().name(),
                m.isCompleted(),
                m.getCompletedAt() != null ? Timestamp.valueOf(m.getCompletedAt()) : null,
                Timestamp.valueOf(m.getExpiredAt())
            ))
            .toArray();

        jdbcTemplate.update(sql, params);
    }
}
