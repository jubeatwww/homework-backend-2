package com.example.demo.common.cqrs.command;

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}
