package com.example.demo.context.mission.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionType {

    CONSECUTIVE_LOGIN("Log in for 3 consecutive days"),
    DIFFERENT_GAMES("Launch at least 3 different games"),
    PLAY_SCORE("Play at least 3 game sessions with combined score over 1,000");

    private final String description;
}
