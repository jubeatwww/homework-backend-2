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
    public Mission save(Mission mission) {
        MissionEntity saved = missionEntityRepository.save(MissionEntity.fromDomain(mission));
        return saved.toDomain();
    }

    @Override
    public void createAllIfAbsent(List<Mission> missions) {
        if (missions.isEmpty()) {
            return;
        }
        String valuePlaceholders = String.join(", ", missions.stream()
            .map(m -> "(?, ?, ?, ?, ?, ?, ?, ?)")
            .toList());
        String sql = "INSERT IGNORE INTO missions (user_id, mission_type, progress, target, completed, completed_at, expired_at, version) VALUES "
            + valuePlaceholders;

        // Version must be 1 (not 0) so that Spring Data JDBC's @Version-based
        // new-entity detection treats rows loaded later as existing (→ UPDATE).
        // Primitive int version == 0 is the "new" sentinel → INSERT → duplicate key.
        Object[] params = missions.stream()
            .flatMap(m -> java.util.stream.Stream.of(
                m.getUserId(),
                m.getMissionType().name(),
                m.getProgress(),
                m.getTarget(),
                m.isCompleted(),
                m.getCompletedAt() != null ? Timestamp.valueOf(m.getCompletedAt()) : null,
                Timestamp.valueOf(m.getExpiredAt()),
                1
            ))
            .toArray();

        jdbcTemplate.update(sql, params);
    }
}
