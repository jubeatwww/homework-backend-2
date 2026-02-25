package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.cache.MissionInitializationCache;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionInitializationService {

    private final MissionRepository missionRepository;
    private final MissionInitializationCache missionInitializationCache;

    public void ensureMissionsExist(Long userId, LocalDateTime expiredAt) {
        try {
            if (missionInitializationCache.isInitialized(userId)) {
                return;
            }
        } catch (Exception e) {
            log.debug("Mission init cache check failed for userId={}: {}", userId, e.getMessage());
        }

        List<Mission> missions = Arrays.stream(MissionType.values())
            .map(type -> Mission.create(userId, type, expiredAt))
            .toList();
        missionRepository.createAllIfAbsent(missions);

        try {
            missionInitializationCache.markInitialized(userId);
        } catch (Exception e) {
            log.debug("Failed to cache mission init for userId={}: {}", userId, e.getMessage());
        }
    }
}
