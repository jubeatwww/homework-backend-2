package com.example.demo.context.mission.domain.exception;

import com.example.demo.context.shared.domain.DomainException;

import java.util.List;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(Long userId) {
        super("USER_NOT_FOUND", List.of(userId));
    }
}