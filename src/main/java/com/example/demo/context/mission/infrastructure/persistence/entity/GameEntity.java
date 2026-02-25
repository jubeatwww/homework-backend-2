package com.example.demo.context.mission.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("games")
public record GameEntity(
    @Id Long id,
    String name,
    LocalDateTime createdAt) {
}