package com.example.demo.context.shared.domain;

import java.io.Serializable;

public abstract class BaseEntity<Entity extends BaseEntity<Entity, Id>, Id extends Serializable>
    implements IdentifiedDomainObject<Id> {
}
