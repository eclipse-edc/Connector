/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.message;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Shim from OK HTTP to the future returned by the ids dispatcher.
 */
class FutureCallback<T> implements Callback {
    private CompletableFuture<T> future;
    private Function<Response, T> handler;

    public FutureCallback(CompletableFuture<T> future, Function<Response, T> handler) {
        this.future = future;
        this.handler = handler;
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        future.completeExceptionally(e);
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {
        future.complete(handler.apply(response));
    }
}
