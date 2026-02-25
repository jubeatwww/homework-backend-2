package com.example.demo.context.mission.application.port;

import java.util.function.Supplier;

public interface DistributedLock {

    /**
     * Try to acquire a lock and execute the action within it.
     * If the lock cannot be acquired, returns the fallback value.
     */
    <T> T tryWithLock(String key, long ttlSeconds, Supplier<T> action, T fallback);
}
