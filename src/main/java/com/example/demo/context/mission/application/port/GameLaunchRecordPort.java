package com.example.demo.context.mission.application.port;

public interface GameLaunchRecordPort {

    boolean recordGameLaunch(Long userId, Long gameId);

    int countDistinctGamesLaunched(Long userId);
}
