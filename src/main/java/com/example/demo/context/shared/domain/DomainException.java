package com.example.demo.context.shared.domain;

import lombok.Getter;

import java.util.List;

@Getter
public abstract class DomainException extends RuntimeException {
    private final String code;
    private final List<Object> arguments;

    protected DomainException(String code, List<Object> arguments) {
        super(code);
        this.code = code;
        this.arguments = arguments;
    }
}
