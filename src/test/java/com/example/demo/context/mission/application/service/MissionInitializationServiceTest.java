package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.cache.MissionInitializationCache;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionInitializationServiceTest {

    @Mock
    MissionRepository missionRepository;

    @Mock
    MissionInitializationCache missionInitializationCache;

    @InjectMocks
    MissionInitializationService service;

    private static final Long USER_ID = 1L;
    private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Test
    void ensureMissionsExist_skipsRepoCallWhenCacheHit() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(true);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        verify(missionRepository, never()).createAllIfAbsent(any());
    }

    @Test
    void ensureMissionsExist_createsOneRowPerMissionType() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mission>> captor = ArgumentCaptor.forClass(List.class);
        verify(missionRepository).createAllIfAbsent(captor.capture());

        assertThat(captor.getValue()).hasSize(MissionType.values().length);
    }

    @Test
    void ensureMissionsExist_createsMissionsForCorrectUser() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mission>> captor = ArgumentCaptor.forClass(List.class);
        verify(missionRepository).createAllIfAbsent(captor.capture());

        assertThat(captor.getValue())
            .extracting(Mission::getUserId)
            .containsOnly(USER_ID);
    }

    @Test
    void ensureMissionsExist_createsMissionsWithAllMissionTypes() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mission>> captor = ArgumentCaptor.forClass(List.class);
        verify(missionRepository).createAllIfAbsent(captor.capture());

        assertThat(captor.getValue())
            .extracting(Mission::getMissionType)
            .containsExactlyInAnyOrder(MissionType.values());
    }

    @Test
    void ensureMissionsExist_createsMissionsWithCorrectExpiry() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Mission>> captor = ArgumentCaptor.forClass(List.class);
        verify(missionRepository).createAllIfAbsent(captor.capture());

        assertThat(captor.getValue())
            .extracting(Mission::getExpiredAt)
            .containsOnly(EXPIRED_AT);
    }

    @Test
    void ensureMissionsExist_marksCacheAfterCreation() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        verify(missionInitializationCache).markInitialized(USER_ID);
    }

    @Test
    void ensureMissionsExist_proceedsWhenCacheCheckThrows() {
        when(missionInitializationCache.isInitialized(USER_ID))
            .thenThrow(new RuntimeException("Redis down"));

        service.ensureMissionsExist(USER_ID, EXPIRED_AT);

        verify(missionRepository).createAllIfAbsent(any());
    }

    @Test
    void ensureMissionsExist_doesNotThrowWhenCacheMarkFails() {
        when(missionInitializationCache.isInitialized(USER_ID)).thenReturn(false);
        doThrow(new RuntimeException("Redis down"))
            .when(missionInitializationCache).markInitialized(USER_ID);

        assertThatNoException().isThrownBy(
            () -> service.ensureMissionsExist(USER_ID, EXPIRED_AT)
        );
        verify(missionRepository).createAllIfAbsent(any());
    }
}