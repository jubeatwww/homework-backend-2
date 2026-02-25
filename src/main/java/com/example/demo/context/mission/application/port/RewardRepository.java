package com.example.demo.context.mission.application.port;

public interface RewardRepository {

    boolean grantReward(Long userId, int points);

    boolean isRewarded(Long userId);
}
