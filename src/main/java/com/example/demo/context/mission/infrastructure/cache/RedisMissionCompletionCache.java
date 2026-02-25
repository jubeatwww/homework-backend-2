package com.example.demo.context.mission.infrastructure.cache;

import com.example.demo.context.mission.application.port.cache.MissionCompletionCache;
import com.example.demo.context.mission.domain.model.MissionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisMissionCompletionCache implements MissionCompletionCache {

    private static final long TTL_SECONDS = Duration.ofDays(30).toSeconds();
    private static final String ALL_COMPLETED = "__ALL__";

    private static final RedisScript<Long> MARK_COMPLETED_SCRIPT = RedisScript.of("""
        local key = KEYS[1]
        local missionType = ARGV[1]
        local ttl = tonumber(ARGV[2])
        redis.call('SADD', key, missionType)
        redis.call('EXPIRE', key, ttl)
        return 1
        """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isCompleted(Long userId, MissionType missionType) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(key(userId), missionType.name())
        );
    }

    @Override
    public void markCompleted(Long userId, MissionType missionType) {
        redisTemplate.execute(
            MARK_COMPLETED_SCRIPT,
            List.of(key(userId)),
            missionType.name(),
            String.valueOf(TTL_SECONDS)
        );
    }

    @Override
    public boolean isAllCompleted(Long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(key(userId), ALL_COMPLETED)
        );
    }

    @Override
    public void markAllCompleted(Long userId) {
        redisTemplate.execute(
            MARK_COMPLETED_SCRIPT,
            List.of(key(userId)),
            ALL_COMPLETED,
            String.valueOf(TTL_SECONDS)
        );
    }

    private String key(Long userId) {
        return "mission:completed:" + userId;
    }
}
