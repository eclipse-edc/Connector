/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.ids.message;

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
public class FutureCallback<T> implements Callback {
    private final CompletableFuture<T> future;
    private final Function<Response, T> handler;

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
        try (response) {
            var result = handler.apply(response);
            future.complete(result);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }
}
