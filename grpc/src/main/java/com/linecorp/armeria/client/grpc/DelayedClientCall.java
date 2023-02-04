package com.linecorp.armeria.client.grpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import com.linecorp.armeria.client.Client;
import com.linecorp.armeria.common.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

final class DelayedClientCall<I,O> extends ClientCall<I,O> {

    private static final List<?> NOOP_TASKS = ImmutableList.of();

    private List<Consumer<ClientCall<I,O>>> pendingTasks = new ArrayList<>();

    @Nullable
    private ClientCall<I,O> delegate;
    private boolean callClosed;

    DelayedClientCall(MethodDescriptor<I, O> method, CompletableFuture<ClientCall<I, O>> future) {

        future.handleAsync((delegate, cause) -> {
            this.delegate = delegate;
            try {
                for (Consumer<ClientCall<I, O>> task : pendingTasks) {
                    task.accept(delegate);
                }
            } catch (Throwable ex) {
                callClosed = true;
            }
            //noinspection unchecked
            pendingTasks = (List<Consumer<ClientCall<I, O>>>) NOOP_TASKS;
            return null;
        });
    }

    @Override
    public void start(Listener<O> responseListener, Metadata headers) {
        if (callClosed) {
            return;
        }

        if (pendingTasks == NOOP_TASKS) {
            delegate.start(responseListener, headers);
        } else {
            pendingTasks.add(ClientCall::start);
        }


    }

    @Override
    public void request(int numMessages) {

    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {

    }

    @Override
    public void halfClose() {

    }

    @Override
    public void sendMessage(I message) {

    }
}
