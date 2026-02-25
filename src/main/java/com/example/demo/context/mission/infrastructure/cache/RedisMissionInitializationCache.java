package com.example.demo.context.mission.infrastructure.cache;

import com.example.demo.context.mission.application.port.cache.MissionInitializationCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisMissionInitializationCache implements MissionInitializationCache {

    private static final String KEY_PREFIX = "mission:init:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isInitialized(Long userId) {
        return redisTemplate.hasKey(KEY_PREFIX + userId);
    }

    @Override
    public void markInitialized(Long userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL);
    }
}
