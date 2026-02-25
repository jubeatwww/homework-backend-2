package com.example.demo.context.mission.domain.event;

public record GameLaunchedEvent(Long userId, Long gameId, long occurredAt) implements UserActionEvent {

    public GameLaunchedEvent(Long userId, Long gameId) {
        this(userId, gameId, System.currentTimeMillis());
    }

    @Override
    public String dedupKey() {
        return userId + ":" + gameId;
    }
}
