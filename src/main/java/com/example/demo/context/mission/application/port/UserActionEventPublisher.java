package com.example.demo.context.mission.application.port;

import com.example.demo.context.mission.domain.event.UserActionEvent;

public interface UserActionEventPublisher {
    void publish(UserActionEvent event);
}
