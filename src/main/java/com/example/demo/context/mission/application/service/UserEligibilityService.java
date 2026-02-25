package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.cache.UserEligibilityCache;
import com.example.demo.context.mission.application.port.UserQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEligibilityService {

    private final Clock clock;
    private final UserEligibilityCache userEligibilityCache;
    private final UserQueryPort userQueryPort;

    public boolean isEligible(Long userId) {
        // 1. Fast cache check
        try {
            if (userEligibilityCache.isExpired(userId)) {
                log.debug("User {} expired (cached)", userId);
                return false;
            }
        } catch (Exception e) {
            log.debug("Cache check failed for userId={}: {}", userId, e.getMessage());
        }

        // 2. DB fallback
        Optional<LocalDateTime> createdAt = userQueryPort.getUserCreatedAt(userId);
        if (createdAt.isPresent() && createdAt.get().plusDays(30).isBefore(LocalDateTime.now(clock))) {
            log.debug("User {} registered > 30 days ago, marking expired", userId);
            try {
                userEligibilityCache.markExpired(userId);
            } catch (Exception e) {
                log.debug("Failed to cache expired for userId={}: {}", userId, e.getMessage());
            }
            return false;
        }

        return true;
    }
}
