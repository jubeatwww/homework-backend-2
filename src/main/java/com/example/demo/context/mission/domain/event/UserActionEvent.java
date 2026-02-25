package com.example.demo.context.mission.domain.event;

import com.example.demo.context.shared.domain.DomainEvent;

public sealed interface UserActionEvent extends DomainEvent
    permits UserLoggedInEvent, GameLaunchedEvent, GamePlayedEvent {

    Long userId();

    default String eventName() {
        return EVENT_NAME_CACHE.get(getClass());
    }

    String dedupKey();

    default String eventKey() {
        return eventName() + ":" + dedupKey();
    }

    ClassValue<String> EVENT_NAME_CACHE = new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
            String name = type.getSimpleName().replace("Event", "");
            return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
        }
    };
}
