package com.example.demo.context.mission.application.service;

import com.example.demo.context.mission.application.port.*;
import com.example.demo.context.mission.application.port.cache.MissionCompletionCache;
import com.example.demo.context.mission.domain.event.RewardGrantedEvent;
import com.example.demo.context.mission.domain.model.Mission;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionProgressService {

    private static final int MAX_RETRIES = 3;
    private static final int REWARD_LOCK_MAX_RETRIES = 3;
    private static final long REWARD_LOCK_TTL_SECONDS = 10;
    private static final long REWARD_LOCK_RETRY_DELAY_MS = 200;

    private final Clock clock;
    private final MissionRepository missionRepository;
    private final LoginRecordPort loginRecordPort;
    private final GameLaunchRecordPort gameLaunchRecordPort;
    private final GamePlayRecordPort gamePlayRecordPort;
    private final MissionCompletionCache missionCompletionCache;
    private final RewardEventPublisher rewardEventPublisher;
    private final RewardRepository rewardRepository;
    private final TransactionTemplate transactionTemplate;
    private final DistributedLock distributedLock;

    public void processLogin(Long userId, LocalDate loginDate) {
        processMission(
            userId,
            MissionType.CONSECUTIVE_LOGIN,
            () -> loginRecordPort.recordLogin(userId, loginDate),
            () -> loginRecordPort.countConsecutiveLoginDays(userId, loginDate)
        );
    }

    public void processGameLaunch(Long userId, Long gameId) {
        processMission(
            userId,
            MissionType.DIFFERENT_GAMES,
            () -> gameLaunchRecordPort.recordGameLaunch(userId, gameId),
            () -> gameLaunchRecordPort.countDistinctGamesLaunched(userId)
        );
    }

    public void processGamePlay(Long userId, Long gameId, int score, String idempotencyKey) {
        processMission(
            userId,
            MissionType.PLAY_SCORE,
            () -> gamePlayRecordPort.recordGamePlay(userId, gameId, score, idempotencyKey),
            () -> gamePlayRecordPort.sumPlayScores(userId),
            () -> gamePlayRecordPort.countPlaySessions(userId) >= 3
        );
    }

    private void processMission(
        Long userId,
        MissionType type,
        BooleanSupplier recordAction,
        IntSupplier progressSupplier
    ) {
        processMission(userId, type, recordAction, progressSupplier, () -> true);
    }

    private void processMission(
        Long userId,
        MissionType type,
        BooleanSupplier recordAction,
        IntSupplier progressSupplier,
        BooleanSupplier completionGuard
    ) {
        if (isCachedCompleted(userId, type)) {
            log.debug("Mission already completed (cached) for userId={}, type={}, checking reward", userId, type);
            tryGrantReward(userId);
            return;
        }

        if (!recordAction.getAsBoolean()) {
            log.debug("Duplicate record for userId={}, type={}, skipping progress update", userId, type);
            tryGrantReward(userId);
            return;
        }

        boolean completed = retryOnOptimisticLock(
            () -> updateMissionProgress(userId, type, progressSupplier, completionGuard),
            () -> log.warn("Mission update failed after {} retries for userId={}, type={}", MAX_RETRIES, userId, type)
        );

        if (completed) {
            safeMarkCompleted(userId, type);
        }
        tryGrantReward(userId);
    }

    private boolean updateMissionProgress(
        Long userId, MissionType type, IntSupplier progressSupplier, BooleanSupplier completionGuard
    ) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status ->
            missionRepository.findByUserIdAndMissionType(userId, type)
                .map(mission -> {
                    if (mission.isCompleted()) {
                        return false;
                    }
                    int newProgress = progressSupplier.getAsInt();
                    boolean canComplete = newProgress >= mission.getTarget() && completionGuard.getAsBoolean();
                    int beforeProgress = mission.getProgress();
                    boolean beforeCompleted = mission.isCompleted();
                    mission.advanceProgress(newProgress, canComplete, LocalDateTime.now(clock));
                    if (mission.getProgress() == beforeProgress && mission.isCompleted() == beforeCompleted) {
                        return false;
                    }
                    missionRepository.save(mission);
                    return mission.isCompleted();
                })
                .orElse(false)
        ));
    }

    private <T> T retryOnOptimisticLock(Supplier<T> action, Runnable onExhausted) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException e) {
                if (attempt >= MissionProgressService.MAX_RETRIES) {
                    onExhausted.run();
                    throw e;
                }
                log.debug("Optimistic lock conflict, retrying ({}/{})", attempt, MissionProgressService.MAX_RETRIES);
            }
        }
    }

    private void tryGrantReward(Long userId) {
        if (isCachedAllCompleted(userId)) {
            return;
        }

        String lockKey = "lock:reward:" + userId;
        for (int attempt = 1; attempt <= REWARD_LOCK_MAX_RETRIES; attempt++) {
            Boolean acquired = distributedLock.tryWithLock(
                lockKey, REWARD_LOCK_TTL_SECONDS,
                () -> { grantRewardIfAllCompleted(userId); return true; },
                null
            );
            if (Boolean.TRUE.equals(acquired)) {
                return;
            }

            log.debug("Failed to acquire reward lock for userId={}, attempt {}/{}", userId, attempt, REWARD_LOCK_MAX_RETRIES);
            sleepBeforeRetry(attempt, userId);
        }

        throw new RuntimeException(
            "Failed to acquire reward lock after " + REWARD_LOCK_MAX_RETRIES + " retries for userId=" + userId);
    }

    private void grantRewardIfAllCompleted(Long userId) {
        List<Mission> missions = missionRepository.findByUserId(userId);
        boolean allCompleted = missions.size() == MissionType.values().length
            && missions.stream().allMatch(Mission::isCompleted);

        if (!allCompleted) {
            return;
        }

        boolean granted = rewardRepository.grantReward(userId, 777);
        if (!granted) {
            log.debug("Reward already granted for userId={}, skipping event", userId);
        } else {
            safeRun(() -> rewardEventPublisher.publish(new RewardGrantedEvent(userId, 777)),
                () -> log.info("Reward granted and event dispatched for userId={}", userId),
                e -> log.warn("Reward granted but event dispatch failed for userId={}: {}", userId, e.getMessage()));
        }

        safeRun(() -> missionCompletionCache.markAllCompleted(userId));
    }

    // ---- cache helpers ----

    private boolean isCachedCompleted(Long userId, MissionType type) {
        try {
            return missionCompletionCache.isCompleted(userId, type);
        } catch (Exception e) {
            log.debug("Cache check failed for userId={}, type={}: {}", userId, type, e.getMessage());
            return false;
        }
    }

    private boolean isCachedAllCompleted(Long userId) {
        try {
            return missionCompletionCache.isAllCompleted(userId);
        } catch (Exception e) {
            log.debug("All-completed cache check failed for userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    private void safeMarkCompleted(Long userId, MissionType type) {
        safeRun(() -> missionCompletionCache.markCompleted(userId, type));
    }

    private void safeRun(Runnable action) {
        try { action.run(); } catch (Exception e) { log.debug("Safe action failed: {}", e.getMessage()); }
    }

    private void safeRun(Runnable action, Runnable onSuccess, Consumer<Exception> onFailure) {
        try { action.run(); onSuccess.run(); } catch (Exception e) { onFailure.accept(e); }
    }

    private void sleepBeforeRetry(int attempt, Long userId) {
        if (attempt >= MissionProgressService.REWARD_LOCK_MAX_RETRIES) return;
        try {
            Thread.sleep(MissionProgressService.REWARD_LOCK_RETRY_DELAY_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while retrying reward lock for userId=" + userId, ie);
        }
    }
}
