package com.example.demo.context.mission.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("games_play_record")
public record GamePlayRecordEntity(
    @Id Long id,
    Long userId,
    Long gameId,
    int score,
    String idempotencyKey,
    LocalDateTime playedAt
) {
}