package com.example.demo.common.cqrs.command;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class ValidatorHolder {

    private static Validator validator;

    ValidatorHolder(Validator validator) {
        ValidatorHolder.validator = validator;
    }

    public static Validator get() {
        return validator;
    }
}