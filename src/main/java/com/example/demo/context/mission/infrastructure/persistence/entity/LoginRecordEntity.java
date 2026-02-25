package com.example.demo.context.mission.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Table("login_records")
public record LoginRecordEntity(
    @Id Long id,
    Long userId,
    LocalDate loginDate
) {
}