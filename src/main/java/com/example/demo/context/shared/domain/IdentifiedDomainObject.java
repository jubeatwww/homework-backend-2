package com.example.demo.context.shared.domain;

import java.io.Serializable;

public interface IdentifiedDomainObject<Id extends Serializable> extends DomainObject {
    Id getId();

    default boolean sameIdAs(IdentifiedDomainObject<Id> other) {
        return getId() != null && getId().equals(other.getId());
    }
}
