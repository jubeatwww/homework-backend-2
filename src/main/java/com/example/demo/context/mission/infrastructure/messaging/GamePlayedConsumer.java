package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.GameQueryPort;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.GamePlayedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "game-played", consumerGroup = "mission-game-play-group")
public class GamePlayedConsumer implements RocketMQListener<GamePlayedEvent> {

    private final UserQueryPort userQueryPort;
    private final GameQueryPort gameQueryPort;
    private final UserEligibilityService userEligibilityService;
    private final MissionProgressService missionProgressService;

    @Override
    public void onMessage(GamePlayedEvent event) {
        log.debug("Consumed GamePlayedEvent: userId={}, gameId={}, score={}, idempotencyKey={}",
            event.userId(), event.gameId(), event.score(), event.idempotencyKey());
        if (event.idempotencyKey() == null || event.idempotencyKey().isBlank()) {
            log.warn("Skip GamePlayedEvent due to missing idempotencyKey: userId={}, gameId={}",
                event.userId(), event.gameId());
            return;
        }
        if (!userQueryPort.userExists(event.userId()) || !gameQueryPort.gameExists(event.gameId())) {
            log.warn("Skip GamePlayedEvent due to missing reference: userId={}, gameId={}", event.userId(), event.gameId());
            return;
        }
        if (!userEligibilityService.isEligible(event.userId())) {
            return;
        }
        missionProgressService.processGamePlay(event.userId(), event.gameId(), event.score(), event.idempotencyKey());
    }
}
