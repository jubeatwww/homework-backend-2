package com.example.demo.common.cqrs.query;

import org.springframework.stereotype.Component;

@Component
public class QueryBus {
    private final QueryHandlerRegistry registry;

    public QueryBus(QueryHandlerRegistry registry) {
        this.registry = registry;
    }

    public <R> R execute(Query<R> query) {
        @SuppressWarnings("unchecked")
        var handler = (QueryHandler<Query<R>, R>) registry.getHandler(query.getClass());
        return handler.handle(query);
    }
}
