package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.GamePlayRecordEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface GamePlayRecordEntityRepository extends Repository<GamePlayRecordEntity, Long> {

    @Modifying
    @Query("INSERT IGNORE INTO games_play_record (user_id, game_id, score, idempotency_key) VALUES (:userId, :gameId, :score, :idempotencyKey)")
    boolean insertIgnore(@Param("userId") Long userId, @Param("gameId") Long gameId, @Param("score") int score, @Param("idempotencyKey") String idempotencyKey);

    @Query("SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = :userId")
    int sumScoresByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(*) FROM games_play_record WHERE user_id = :userId")
    int countByUserId(@Param("userId") Long userId);
}
