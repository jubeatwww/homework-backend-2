package com.example.demo.context.mission.domain.exception;

import com.example.demo.context.shared.domain.DomainException;

import java.util.List;

public class GameNotFoundException extends DomainException {

    public GameNotFoundException(Long gameId) {
        super("GAME_NOT_FOUND", List.of(gameId));
    }
}