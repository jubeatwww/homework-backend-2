package com.example.demo.common.cqrs.command;

import com.example.demo.common.cqrs.AbstractHandlerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandHandlerRegistry extends AbstractHandlerRegistry<CommandHandler<?, ?>> {
    @SuppressWarnings("unchecked")
    public CommandHandlerRegistry(List<CommandHandler<?, ?>> handlers) {
        super((Class<CommandHandler<?, ?>>) (Class<?>) CommandHandler.class, "CommandHandler", handlers);
    }
}
