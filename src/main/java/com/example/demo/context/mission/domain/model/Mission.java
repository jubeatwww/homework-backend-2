package com.example.demo.context.mission.domain.model;

import com.example.demo.context.shared.domain.BaseAggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Mission extends BaseAggregateRoot<Mission, Long> {

    private Long id;
    private Long userId;
    private MissionType missionType;
    private int progress;
    private int target;
    private boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime expiredAt;
    private int version;

    public static Mission create(Long userId, MissionType missionType, LocalDateTime expiredAt) {
        var mission = new Mission();
        mission.userId = userId;
        mission.missionType = missionType;
        mission.progress = 0;
        mission.target = missionType.getTarget();
        mission.completed = false;
        mission.expiredAt = expiredAt;
        mission.version = 0;
        return mission;
    }

    public static Mission reconstitute(
        Long id, Long userId,
        MissionType missionType,
        int progress,
        int target,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime expiredAt,
        int version) {
        var mission = new Mission();
        mission.id = id;
        mission.userId = userId;
        mission.missionType = missionType;
        mission.progress = progress;
        mission.target = target;
        mission.completed = completed;
        mission.completedAt = completedAt;
        mission.expiredAt = expiredAt;
        mission.version = version;
        return mission;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiredAt != null && now.isAfter(expiredAt);
    }

    public void advanceProgress(int newProgress, LocalDateTime now) {
        advanceProgress(newProgress, true, now);
    }

    public void advanceProgress(int newProgress, boolean canComplete, LocalDateTime now) {
        if (this.completed || isExpired(now)) {
            return;
        }
        if (newProgress > this.progress) {
            this.progress = newProgress;
        }
        if (canComplete && this.progress >= this.target) {
            this.completed = true;
            this.completedAt = now;
        }
    }
}
