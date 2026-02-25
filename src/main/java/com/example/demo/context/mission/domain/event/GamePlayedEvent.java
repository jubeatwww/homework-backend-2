package com.example.demo.context.mission.domain.event;

public record GamePlayedEvent(Long userId, Long gameId, int score, String idempotencyKey,
                              long occurredAt) implements UserActionEvent {

    public GamePlayedEvent(Long userId, Long gameId, int score, String idempotencyKey) {
        this(userId, gameId, score, idempotencyKey, System.currentTimeMillis());
    }

    @Override
    public String dedupKey() {
        return userId + ":" + idempotencyKey;
    }
}
