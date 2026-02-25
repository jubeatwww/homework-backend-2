package com.example.demo.context.mission.infrastructure.persistence.entity;

import com.example.demo.context.mission.domain.model.MissionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MissionEntityTest {

    @Test
    void isNew_returnsTrueWhenIdIsNull() {
        MissionEntity entity = new MissionEntity(
            null, 1L, MissionType.CONSECUTIVE_LOGIN,
            false, null, LocalDateTime.now().plusDays(30)
        );

        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void isNew_returnsFalseWhenIdExists() {
        MissionEntity entity = new MissionEntity(
            10L, 1L, MissionType.CONSECUTIVE_LOGIN,
            false, null, LocalDateTime.now().plusDays(30)
        );

        assertThat(entity.isNew()).isFalse();
    }
}
