package com.example.demo.context.mission.infrastructure.cache;

import com.example.demo.context.mission.application.port.cache.UserEligibilityCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUserEligibilityCache implements UserEligibilityCache {

    private static final String KEY_PREFIX = "user:expired:";
    private static final String EXPIRED_VALUE = "1";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isExpired(Long userId) {
        return redisTemplate.hasKey(KEY_PREFIX + userId);
    }

    @Override
    public void markExpired(Long userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, EXPIRED_VALUE, TTL);
    }
}
