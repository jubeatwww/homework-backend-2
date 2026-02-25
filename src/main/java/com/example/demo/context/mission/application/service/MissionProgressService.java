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
import java.util.function.IntSupplier;

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

    private void processMission(Long userId, MissionType type, BooleanSupplier recordAction, IntSupplier progressSupplier) {
        processMission(userId, type, recordAction, progressSupplier, () -> true);
    }

    private void processMission(
        Long userId,
        MissionType type,
        BooleanSupplier recordAction,
        IntSupplier progressSupplier,
        BooleanSupplier completionGuard
    ) {
        boolean alreadyCompleted = false;
        try {
            alreadyCompleted = missionCompletionCache.isCompleted(userId, type);
        } catch (Exception e) {
            log.debug("Cache check failed for userId={}, type={}: {}", userId, type, e.getMessage());
        }

        if (alreadyCompleted) {
            // Mission already done (possibly from a previous MQ delivery whose
            // tryGrantReward failed). Skip record/progress but still check reward.
            log.debug("Mission already completed (cached) for userId={}, type={}, checking reward", userId, type);
            tryGrantReward(userId);
            return;
        }

        boolean isNewRecord = recordAction.getAsBoolean();
        if (!isNewRecord) {
            log.debug("Duplicate record for userId={}, type={}, skipping progress update", userId, type);
            tryGrantReward(userId);
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Boolean completed = transactionTemplate.execute(status ->
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
                );

                if (Boolean.TRUE.equals(completed)) {
                    safeMarkCompleted(userId, type);
                }
                // Always check reward — even if this mission didn't just complete,
                // another mission may have completed concurrently.
                tryGrantReward(userId);
                return;
            } catch (OptimisticLockingFailureException e) {
                if (attempt == MAX_RETRIES) {
                    log.warn("Mission update failed after {} retries for userId={}, type={}", MAX_RETRIES, userId, type);
                    throw e;
                }
                log.debug("Optimistic lock conflict for userId={}, type={}, retrying ({}/{})", userId, type, attempt, MAX_RETRIES);
            }
        }
    }

    private void tryGrantReward(Long userId) {
        try {
            if (missionCompletionCache.isAllCompleted(userId)) {
                return;
            }
        } catch (Exception e) {
            log.debug("All-completed cache check failed for userId={}: {}", userId, e.getMessage());
        }

        String lockKey = "lock:reward:" + userId;
        for (int attempt = 1; attempt <= REWARD_LOCK_MAX_RETRIES; attempt++) {
            // Lambda returns TRUE when lock acquired & action executed;
            // fallback null means lock was NOT acquired.
            Boolean lockAcquired = distributedLock.tryWithLock(lockKey, REWARD_LOCK_TTL_SECONDS, () -> {
                List<Mission> missions = missionRepository.findByUserId(userId);
                boolean allCompleted = missions.size() == MissionType.values().length
                    && missions.stream().allMatch(Mission::isCompleted);

                if (!allCompleted) {
                    return true;
                }

                boolean granted = rewardRepository.grantReward(userId, 777);
                if (!granted) {
                    log.debug("Reward already granted for userId={}, skipping event", userId);
                    safeMarkAllCompleted(userId);
                    return true;
                }

                try {
                    rewardEventPublisher.publish(new RewardGrantedEvent(userId, 777));
                    log.info("Reward granted and event dispatched for userId={}", userId);
                } catch (Exception e) {
                    log.warn("Reward granted but event dispatch failed for userId={}: {}", userId, e.getMessage());
                }

                safeMarkAllCompleted(userId);
                return true;
            }, null);

            if (Boolean.TRUE.equals(lockAcquired)) {
                return;
            }

            log.debug("Failed to acquire reward lock for userId={}, attempt {}/{}",
                userId, attempt, REWARD_LOCK_MAX_RETRIES);

            if (attempt < REWARD_LOCK_MAX_RETRIES) {
                try {
                    Thread.sleep(REWARD_LOCK_RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(
                        "Interrupted while retrying reward lock for userId=" + userId, ie);
                }
            }
        }

        // All retries exhausted — throw so the MQ consumer fails and redelivers
        throw new RuntimeException(
            "Failed to acquire reward lock after " + REWARD_LOCK_MAX_RETRIES
                + " retries for userId=" + userId);
    }

    private void safeMarkAllCompleted(Long userId) {
        try {
            missionCompletionCache.markAllCompleted(userId);
        } catch (Exception e) {
            log.debug("Failed to mark all-completed in cache for userId={}", userId);
        }
    }

    private void safeMarkCompleted(Long userId, MissionType type) {
        try {
            missionCompletionCache.markCompleted(userId, type);
        } catch (Exception e) {
            log.debug("Failed to mark mission completed in cache: userId={}, type={}", userId, type);
        }
    }
}
