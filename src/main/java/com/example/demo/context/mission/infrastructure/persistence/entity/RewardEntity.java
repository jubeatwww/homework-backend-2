package com.example.demo.context.mission.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("rewards")
public record RewardEntity(
    @Id Long id,
    Long userId,
    int points,
    LocalDateTime rewardedAt
) {
}