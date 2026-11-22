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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.http.spi;

import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * HTTP client service, it wraps {@link okhttp3.OkHttpClient} and provides retry mechanism out of the box.
 */
public interface EdcHttpClient {

    /**
     * Executes the specified request synchronously.
     *
     * @param request the {@link Request}.
     * @return a {@link Response}, must be closed explicitly after consumption
     * @throws IOException on connection error.
     */
    Response execute(Request request) throws IOException;

    /**
     * Executes the specified request synchronously.
     * Accepts a list of {@link FallbackFactories} that could apply retry in particular occasions.
     *
     * @param request the {@link Request}.
     * @param fallbacks a list of fallbacks to be applied.
     * @return a {@link Response}, must be closed explicitly after consumption
     * @throws IOException on connection error.
     */
    Response execute(Request request, List<FallbackFactory> fallbacks) throws IOException;

    /**
     * Executes the specified request synchronously applying the mapping function to the response.
     *
     * @param request the {@link Request}.
     *                @param mappingFunction the function that will be applied to the {@link Response}.
     * @return a {@link Result}, containing the object returned by the mappingFunction
     */
    <T> Result<T> execute(Request request, Function<Response, Result<T>> mappingFunction);

    /**
     * Executes the specified request synchronously applying the mapping function to the response.
     * Accepts a list of {@link FallbackFactories} that could apply retry in particular occasions.
     *
     * @param request the {@link Request}.
     * @param fallbacks a list of fallbacks to be applied.
     * @param mappingFunction the function that will be applied to the {@link Response}.
     * @return a {@link Result}, containing the object returned by the mappingFunction
     */
    <T> Result<T> execute(Request request, List<FallbackFactory> fallbacks, Function<Response, Result<T>> mappingFunction);

    /**
     * Executes the specified request asynchronously and returns the response.
     * Accepts a list of {@link FallbackFactories} that could apply retry in particular occasions.
     *
     * @param request the {@link Request}.
     * @param fallbacks a list of fallbacks to be applied.
     * @return a {@link CompletableFuture} containing the {@link Response} instance. If the server isn't available,
     *         fails with a {@link java.net.ConnectException}.
     */
    CompletableFuture<Response> executeAsync(Request request, List<FallbackFactory> fallbacks);

    /**
     * Returns a new client instance with a custom dns server set.
     *
     * @param dnsServer the url of a dns server
     * @return a new client instance with the dns server set.
     */
    EdcHttpClient withDns(String dnsServer);

    /**
     * Returns a new client instance whose calls must complete within the given time, including connecting, writing
     * the request and reading the whole response. Each retry attempt gets its own call timeout.
     * <p>
     * Implementations should override this method: the default implementation returns the same client.
     *
     * @param callTimeout the maximum duration of a single call, a zero duration means no limit.
     * @return a new client instance with the call timeout set.
     */
    default EdcHttpClient withCallTimeout(Duration callTimeout) {
        return this;
    }

    /**
     * Returns a new client instance that retries a failed call at most the given number of times.
     * <p>
     * Implementations should override this method: the default implementation returns the same client.
     *
     * @param maxRetries the maximum number of retries, 0 means no retry.
     * @return a new client instance with the maximum number of retries set.
     */
    default EdcHttpClient withMaxRetries(int maxRetries) {
        return this;
    }

}
