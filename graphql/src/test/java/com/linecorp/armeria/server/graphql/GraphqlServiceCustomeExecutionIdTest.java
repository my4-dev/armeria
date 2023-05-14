package com.linecorp.armeria.server.graphql;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import graphql.execution.ExecutionId;
import graphql.schema.DataFetcher;

public class GraphqlServiceCustomeExecutionIdTest {

    private static final ExecutionIdGenerator customExecutionIdGenerator = (ctx, query, operationName) -> {
        String customId = "custom_" + ctx.id().text();
        return ExecutionId.from(customId);
    };

    @RegisterExtension
     static ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            final File graphqlSchemaFile = new File(getClass().getResource("/test.graphqls").toURI());
            final GraphqlService service =
                    GraphqlService.builder()
                                  .schemaFile(graphqlSchemaFile)
                                  .executionIdGenerator(customExecutionIdGenerator)
                                  .runtimeWiring(c -> {
                                      final DataFetcher<String> bar = dataFetcher("bar");
                                      c.type("Query",
                                             typeWiring -> typeWiring.dataFetcher("foo", bar));
                                  })
                                  .build();
            sb.service("/graphql", service);
        }
    };

    private static DataFetcher<String> dataFetcher(String value) {
        return environment -> {
            final ServiceRequestContext ctx = GraphqlServiceContexts.get(environment);
            assertThat(ctx.eventLoop().inEventLoop()).isFalse();
            // Make sure that a ServiceRequestContext is available
            assertThat(ServiceRequestContext.current()).isSameAs(ctx);

            // Make sure that the custom execution id is used
            assertThat(environment.getExecutionId().toString()).startsWith("custom_");

            return value;
        };
    }

    @Test
    void testCustomExecutionId() {
        final AggregatedHttpResponse response = WebClient.of(server.httpUri())
                                                         .get("/graphql?query={foo}")
                                                         .aggregate().join();

        assertThat(response.status()).isEqualTo(HttpStatus.OK);
        assertThatJson(response.contentUtf8()).node("data.foo").isEqualTo("bar");
    }
}
