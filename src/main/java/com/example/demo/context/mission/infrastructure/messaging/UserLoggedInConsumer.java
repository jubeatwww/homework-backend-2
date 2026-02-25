package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.application.service.MissionProgressService;
import com.example.demo.context.mission.application.service.UserEligibilityService;
import com.example.demo.context.mission.domain.event.UserLoggedInEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "user-logged-in", consumerGroup = "mission-login-group")
public class UserLoggedInConsumer implements RocketMQListener<UserLoggedInEvent> {

    private final UserQueryPort userQueryPort;
    private final UserEligibilityService userEligibilityService;
    private final MissionProgressService missionProgressService;

    @Override
    public void onMessage(UserLoggedInEvent event) {
        log.debug("Consumed UserLoggedInEvent: userId={}", event.userId());
        if (!userQueryPort.userExists(event.userId())) {
            log.warn("Skip UserLoggedInEvent due to missing userId={}", event.userId());
            return;
        }
        if (!userEligibilityService.isEligible(event.userId())) {
            return;
        }
        missionProgressService.processLogin(event.userId(), event.loginDate());
    }
}
