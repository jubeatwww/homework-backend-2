package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.GameLaunchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "game-launched", consumerGroup = "mission-game-launch-group")
public class GameLaunchedConsumer implements RocketMQListener<GameLaunchedEvent> {

    private final UserQueryPort userQueryPort;
    private final GameQueryPort gameQueryPort;
    private final UserEligibilityService userEligibilityService;
    private final MissionProgressService missionProgressService;

    @Override
    public void onMessage(GameLaunchedEvent event) {
        log.debug("Consumed GameLaunchedEvent: userId={}, gameId={}", event.userId(), event.gameId());
        if (!userQueryPort.userExists(event.userId()) || !gameQueryPort.gameExists(event.gameId())) {
            log.warn("Skip GameLaunchedEvent due to missing reference: userId={}, gameId={}", event.userId(), event.gameId());
            return;
        }
        if (!userEligibilityService.isEligible(event.userId())) {
            return;
        }
        missionProgressService.processGameLaunch(event.userId(), event.gameId());
    }
}
