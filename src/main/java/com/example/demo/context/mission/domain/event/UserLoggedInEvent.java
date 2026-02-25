package com.example.demo.context.mission.domain.event;

import java.time.LocalDate;

public record UserLoggedInEvent(Long userId, LocalDate loginDate, long occurredAt) implements UserActionEvent {

    public UserLoggedInEvent(Long userId, LocalDate loginDate) {
        this(userId, loginDate, System.currentTimeMillis());
    }

    @Override
    public String dedupKey() {
        return userId + ":" + loginDate;
    }
}
