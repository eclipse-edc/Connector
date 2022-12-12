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

package org.eclipse.edc.spi.http;

import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
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
     * Executes the specified request asynchronously, maps the response with the mappingFunction.
     *
     * @param request the {@link Request}.
     * @param mappingFunction the function that will be applied to the {@link Response}.
     * @return a {@link CompletableFuture} containing the result value.
     * @param <T> the result value.
     */
    <T> CompletableFuture<T> executeAsync(Request request, Function<Response, T> mappingFunction);

    /**
     * Returns a new client instance with a custom dns server set.
     *
     * @param dnsServer the url of a dns server
     * @return a new client instance with the dns server set.
     */
    EdcHttpClient withDns(String dnsServer);

}
