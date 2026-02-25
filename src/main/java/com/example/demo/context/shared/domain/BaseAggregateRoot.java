package com.example.demo.context.shared.domain;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class BaseAggregateRoot<AggregateRoot extends BaseAggregateRoot<AggregateRoot, Id>, Id extends Serializable>
    extends BaseEntity<AggregateRoot, Id> {
    @Transient
    private final List<DomainEvent> domainEvents = Collections.synchronizedList(new ArrayList<>());

    @DomainEvents
    protected Collection<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    protected void registerDomainEvent(DomainEvent domainEvent) {
        domainEvents.add(domainEvent);
    }

    @AfterDomainEventPublication
    public void afterDomainEventPublication() {
        domainEvents.clear();
    }
}
