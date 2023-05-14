package com.linecorp.armeria.server.graphql;

import com.linecorp.armeria.server.ServiceRequestContext;

import graphql.execution.ExecutionId;
import graphql.execution.ExecutionIdProvider;

public interface ExecutionIdGenerator {

    static ExecutionIdGenerator of() {
        return (ctx, query, operationName) -> ExecutionId.from(ctx.id().text());
    }

    ExecutionId generate(ServiceRequestContext ctx, String query, String operationName);

    default ExecutionIdProvider asExecutionProvider() {
        return (query, operationName, ctx) -> generate((ServiceRequestContext) ctx, query, operationName);
    }
}
