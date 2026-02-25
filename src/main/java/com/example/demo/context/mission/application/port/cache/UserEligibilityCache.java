package com.example.demo.context.mission.application.port.cache;

public interface UserEligibilityCache {
    boolean isExpired(Long userId);
    void markExpired(Long userId);
}
