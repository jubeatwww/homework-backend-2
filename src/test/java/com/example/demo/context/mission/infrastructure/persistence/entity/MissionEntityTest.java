package com.example.demo.context.mission.infrastructure.persistence.entity;

import com.example.demo.context.mission.domain.model.MissionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MissionEntityTest {

    @Test
    void isNew_returnsTrueWhenIdIsNullEvenIfVersionIsZero() {
        MissionEntity entity = new MissionEntity(
            null, 1L, MissionType.CONSECUTIVE_LOGIN,
            0, 3, false, null, LocalDateTime.now().plusDays(30), 0
        );

        assertThat(entity.isNew()).isTrue();
    }

    @Test
    void isNew_returnsFalseWhenIdExistsEvenIfVersionIsZero() {
        MissionEntity entity = new MissionEntity(
            10L, 1L, MissionType.CONSECUTIVE_LOGIN,
            1, 3, false, null, LocalDateTime.now().plusDays(30), 0
        );

        assertThat(entity.isNew()).isFalse();
    }
}
