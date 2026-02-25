package com.example.demo.context.mission.application.port;

public interface GamePlayRecordPort {

    boolean recordGamePlay(Long userId, Long gameId, int score, String idempotencyKey);

    int sumPlayScores(Long userId);

    int countPlaySessions(Long userId);
}
