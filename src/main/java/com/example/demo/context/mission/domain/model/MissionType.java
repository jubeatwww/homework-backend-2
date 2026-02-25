package com.example.demo.context.mission.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionType {

    CONSECUTIVE_LOGIN(3, "Log in for 3 consecutive days"),
    DIFFERENT_GAMES(3, "Launch at least 3 different games"),
    PLAY_SCORE(1000, "Play at least 3 game sessions with combined score over 1,000");

    private final int target;
    private final String description;

    public boolean isTargetReached(int progress, int target) {
        if (this == PLAY_SCORE) {
            return progress > target;
        }
        return progress >= target;
    }
}
