package com.example.demo.context.mission.infrastructure.persistence.entity;

import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("missions")
public record MissionEntity(
    @Id Long id,
    Long userId,
    MissionType missionType,
    boolean completed,
    LocalDateTime completedAt,
    LocalDateTime expiredAt
) implements Persistable<Long> {

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public Mission toDomain() {
        return Mission.reconstitute(id, userId, missionType, completed, completedAt, expiredAt);
    }

    public static MissionEntity fromDomain(Mission m) {
        return new MissionEntity(
            m.getId(), m.getUserId(), m.getMissionType(),
            m.isCompleted(), m.getCompletedAt(), m.getExpiredAt()
        );
    }
}
