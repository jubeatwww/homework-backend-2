package com.example.demo.context.mission.infrastructure.persistence.repository;

import com.example.demo.context.mission.infrastructure.persistence.entity.RewardEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RewardEntityRepository extends Repository<RewardEntity, Long> {

    @Modifying
    @Query("INSERT IGNORE INTO rewards (user_id, points) VALUES (:userId, :points)")
    boolean insertIgnore(@Param("userId") Long userId, @Param("points") int points);

    @Query("SELECT COUNT(*) FROM rewards WHERE user_id = :userId")
    int countByUserId(@Param("userId") Long userId);
}
