package com.example.demo.common.cqrs.query;

import com.example.demo.common.cqrs.AbstractHandlerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryHandlerRegistry extends AbstractHandlerRegistry<QueryHandler<?, ?>> {
    @SuppressWarnings("unchecked")
    public QueryHandlerRegistry(List<QueryHandler<?, ?>> handlers) {
        super((Class<QueryHandler<?, ?>>) (Class<?>) QueryHandler.class, "QueryHandler", handlers);
    }
}
