/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.protocol.ids.message;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FutureCallbackTest {

    @Test
    void onResponse_completesFutureWithResult() {
        var future = new CompletableFuture<>();
        var futureCallback = new FutureCallback<>(future, response -> "result");

        futureCallback.onResponse(mock(Call.class), successfulResponse());

        assertThat(future).succeedsWithin(10, SECONDS).isEqualTo("result");
    }

    @Test
    void onResponse_failsFutureWhenHandlerThrowsException() {
        var future = new CompletableFuture<>();
        var futureCallback = new FutureCallback<>(future, response -> {
            throw new RuntimeException("an error");
        });

        futureCallback.onResponse(mock(Call.class), successfulResponse());

        assertThat(future).failsWithin(10, SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void onFailure_failsFuture() {
        var future = new CompletableFuture<>();
        var futureCallback = new FutureCallback<>(future, response -> {
            throw new RuntimeException("an error");
        });

        futureCallback.onFailure(mock(Call.class), new IOException());

        assertThat(future).failsWithin(10, SECONDS)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(IOException.class);
    }

    private Response successfulResponse() {
        return new Response.Builder()
                .code(200)
                .body(ResponseBody.create("{}", MediaType.get("application/json")))
                .message("Test message")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://test.some.url").build())
                .build();
    }
}
