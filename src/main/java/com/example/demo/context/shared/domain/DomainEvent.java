package com.example.demo.context.shared.domain;

public interface DomainEvent extends DomainObject {
    long occurredAt();
}
