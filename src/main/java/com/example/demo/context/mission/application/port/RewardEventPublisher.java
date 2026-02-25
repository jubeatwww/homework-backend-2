package com.example.demo.context.mission.application.port;

import com.example.demo.context.mission.domain.event.RewardGrantedEvent;

public interface RewardEventPublisher {
    void publish(RewardGrantedEvent event);
}
