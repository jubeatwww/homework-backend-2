package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.GameLaunchRecordEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface GameLaunchRecordEntityRepository extends Repository<GameLaunchRecordEntity, Long> {

    @Modifying
    @Query("INSERT IGNORE INTO game_launch_records (user_id, game_id) VALUES (:userId, :gameId)")
    boolean insertIgnore(@Param("userId") Long userId, @Param("gameId") Long gameId);

    @Query("SELECT COUNT(DISTINCT game_id) FROM game_launch_records WHERE user_id = :userId")
    int countDistinctGamesByUserId(@Param("userId") Long userId);
}
