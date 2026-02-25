package com.example.demo.context.mission.application.port.cache;

public interface MissionInitializationCache {
    boolean isInitialized(Long userId);

    void markInitialized(Long userId);
}
