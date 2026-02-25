package com.example.demo.context.mission.domain.model;

import com.example.demo.context.shared.domain.BaseAggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Mission extends BaseAggregateRoot<Mission, Long> {

    private Long id;
    private Long userId;
    private MissionType missionType;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;

    public static Mission create(Long userId, MissionType missionType, LocalDateTime expiredAt) {
        var mission = new Mission();
        mission.userId = userId;
        mission.missionType = missionType;
        mission.completed = false;
        mission.expiredAt = expiredAt;
        return mission;
    }

    public static Mission reconstitute(
        Long id, Long userId,
        MissionType missionType,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime expiredAt) {
        var mission = new Mission();
        mission.id = id;
        mission.userId = userId;
        mission.missionType = missionType;
        mission.completed = completed;
        mission.completedAt = completedAt;
        mission.expiredAt = expiredAt;
        return mission;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiredAt != null && now.isAfter(expiredAt);
    }

    /**
     * Completes this mission if not already completed and not expired.
     * This is a one-way latch: once completed, it cannot be undone.
     *
     * @return true if this call actually transitioned the mission to completed
     */
    public boolean complete(LocalDateTime now) {
        if (this.completed || isExpired(now)) {
            return false;
        }
        this.completed = true;
        this.completedAt = now;
        return true;
    }
}
