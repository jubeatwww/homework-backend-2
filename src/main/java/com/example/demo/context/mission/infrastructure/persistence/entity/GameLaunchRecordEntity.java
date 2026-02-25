package com.example.demo.context.mission.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("game_launch_records")
public record GameLaunchRecordEntity(
    @Id Long id,
    Long userId,
    Long gameId,
    LocalDateTime launchedAt
) {
}