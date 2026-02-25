package com.example.demo.common.cqrs;

import org.springframework.aop.support.AopUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public final class HandlerTypeResolver {
    private HandlerTypeResolver() {
    }

    public static Optional<Class<?>> resolve(Object handler, Class<?> handlerInterface) {
        var targetClass = AopUtils.getTargetClass(handler);
        for (var current = targetClass; current != null && current != Object.class; current = current.getSuperclass()) {
            for (var type : current.getGenericInterfaces()) {
                var messageClass = extractFromType(type, handlerInterface);
                if (messageClass.isPresent()) {
                    return messageClass;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Class<?>> extractFromType(Type type, Class<?> handlerInterface) {
        if (type instanceof ParameterizedType pt && pt.getRawType().equals(handlerInterface)) {
            var arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> messageClass) {
                return Optional.of(messageClass);
            }
        }
        return Optional.empty();
    }
}
