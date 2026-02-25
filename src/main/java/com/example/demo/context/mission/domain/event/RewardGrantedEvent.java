package com.example.demo.context.mission.domain.event;

import com.example.demo.context.shared.domain.DomainEvent;

public record RewardGrantedEvent(Long userId, int points, long occurredAt) implements DomainEvent {

    public RewardGrantedEvent(Long userId, int points) {
        this(userId, points, System.currentTimeMillis());
    }
}
