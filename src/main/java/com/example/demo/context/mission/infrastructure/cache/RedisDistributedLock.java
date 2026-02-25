package com.example.demo.context.mission.infrastructure.cache;

import com.example.demo.context.mission.application.port.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock implements DistributedLock {

    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of("""
        if redis.call('GET', KEYS[1]) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
        end
        return 0
        """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public <T> T tryWithLock(String key, long ttlSeconds, Supplier<T> action, T fallback) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(key, token, Duration.ofSeconds(ttlSeconds));

        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Failed to acquire lock: {}", key);
            return fallback;
        }

        try {
            return action.get();
        } finally {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
        }
    }
}
