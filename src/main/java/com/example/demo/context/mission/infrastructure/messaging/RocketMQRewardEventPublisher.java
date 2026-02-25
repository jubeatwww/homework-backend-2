package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.RewardEventPublisher;
import com.example.demo.context.mission.domain.event.RewardGrantedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMQRewardEventPublisher implements RewardEventPublisher {

    private static final String TOPIC = "reward-granted";

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void publish(RewardGrantedEvent event) {
        String key = "reward-granted:" + event.userId();
        var message = MessageBuilder.withPayload(event)
            .setHeader(RocketMQHeaders.KEYS, key)
            .build();

        SendResult result = rocketMQTemplate.syncSend(TOPIC, message);
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            throw new IllegalStateException("Failed to send reward event for userId=" + event.userId());
        }
        log.debug("Sent RewardGrantedEvent to MQ topic={}, key={}", TOPIC, key);
    }
}
