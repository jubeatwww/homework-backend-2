package com.example.demo.common.cqrs.command;

import org.springframework.stereotype.Component;

@Component
public class CommandBus {
    private final CommandHandlerRegistry registry;

    public CommandBus(CommandHandlerRegistry registry) {
        this.registry = registry;
    }

    public <C extends Command<R>, R> R execute(C command) {
        @SuppressWarnings("unchecked")
        var handler = (CommandHandler<C, R>) registry.getHandler(command.getClass());
        return handler.handle(command);
    }
}
