package com.linecorp.armeria.client.grpc;

import java.util.concurrent.CompletableFuture;

import com.linecorp.armeria.common.annotation.Nullable;

import io.grpc.ClientCall;
import io.grpc.ClientCall.Listener;
import io.grpc.MethodDescriptor;

final class DeferredListener<O> extends ClientCall.Listener<O> {

    @Nullable
    private Listener<O> delegate;
    private boolean callClosed;

    DeferredListener(MethodDescriptor<?,O> method, CompletableFuture<ClientCall<?,O>> future) {

        future.handleAsync((delegate, cause) -> {
            if (cause != null) {
                callClosed = true;
                return null;
            }

            this.delegate = delegate;
        });
    }

    @Override
    public void onMessage(O message) {
        if (callClosed) {
            return;
        }

        delegate.onMessage(message);
    }
}
