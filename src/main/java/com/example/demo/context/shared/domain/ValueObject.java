package com.example.demo.context.shared.domain;

public interface ValueObject extends DomainObject {
    @Override
    boolean equals(Object other);
}
