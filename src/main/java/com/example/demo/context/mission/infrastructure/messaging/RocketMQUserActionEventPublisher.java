package com.example.demo.context.mission.infrastructure.messaging;

import com.example.demo.context.mission.application.port.UserActionEventPublisher;
import com.example.demo.context.mission.domain.event.UserActionEvent;
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
public class RocketMQUserActionEventPublisher implements UserActionEventPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void publish(UserActionEvent event) {
        String topic = event.eventName();
        var message = MessageBuilder.withPayload(event)
            .setHeader(RocketMQHeaders.KEYS, event.eventKey())
            .build();

        SendResult result = rocketMQTemplate.syncSend(topic, message);
        if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
            throw new IllegalStateException("Failed to send message for topic=" + topic);
        }
        log.debug("Sent {} to MQ topic={}, key={}",
            event.getClass().getSimpleName(), topic, event.eventKey());
    }
}
