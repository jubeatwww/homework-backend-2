package com.example.demo.common.cqrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractHandlerRegistry<H> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<Class<?>, H> handlers = new HashMap<>();
    private final String handlerTypeName;

    public AbstractHandlerRegistry(
        Class<H> handlerInterface,
        String handlerTypeName,
        List<H> handlerList
    ) {
        this.handlerTypeName = handlerTypeName;
        for (var handler : handlerList) {
            HandlerTypeResolver.resolve(handler, handlerInterface).ifPresentOrElse(
                messageClass -> {
                    var prev = handlers.putIfAbsent(messageClass, handler);
                    if (prev != null) {
                        throw new IllegalStateException(
                            "Duplicate %s for %s: [%s] and [%s]".formatted(
                                handlerTypeName, messageClass.getSimpleName(),
                                prev.getClass().getSimpleName(), handler.getClass().getSimpleName()));
                    }
                    log.info("Registered {}: {} -> {}",
                        handlerTypeName, messageClass.getSimpleName(), handler.getClass().getSimpleName());
                },
                () -> log.warn("Could not resolve message type for {}: {}",
                    handlerTypeName, handler.getClass().getSimpleName())
            );
        }
    }

    public <C> H getHandler(Class<C> messageClass) {
        H handler = handlers.get(messageClass);
        if (handler == null) {
            throw new IllegalArgumentException(
                "No %s registered for %s".formatted(handlerTypeName, messageClass.getSimpleName()));
        }
        return handler;
    }
}
