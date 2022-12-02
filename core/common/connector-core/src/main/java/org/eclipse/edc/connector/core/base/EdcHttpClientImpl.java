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

package org.eclipse.edc.connector.core.base;

import dev.failsafe.RetryPolicy;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;
import org.eclipse.edc.spi.http.EdcHttpClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static dev.failsafe.okhttp.FailsafeCall.with;
import static java.util.Objects.requireNonNull;

public class EdcHttpClientImpl implements EdcHttpClient {
    private final OkHttpClient okHttpClient;
    private final RetryPolicy<Response> retryPolicy;

    public EdcHttpClientImpl(OkHttpClient okHttpClient, RetryPolicy<Response> retryPolicy) {
        this.okHttpClient = okHttpClient;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Response execute(Request request) throws IOException {
        var call = okHttpClient.newCall(request);
        return with(retryPolicy).compose(call).execute();
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(Request request, Function<Response, T> mappingFunction) {
        var call = okHttpClient.newCall(request);
        return with(retryPolicy).compose(call)
                .executeAsync()
                .thenApply(response -> {
                    try (response) {
                        return mappingFunction.apply(response);
                    }
                });
    }

    @Override
    public EdcHttpClient withDns(String dnsServer) {
        var url = requireNonNull(HttpUrl.get(dnsServer));

        var dns = new DnsOverHttps.Builder()
                .client(okHttpClient)
                .url(url)
                .includeIPv6(false)
                .build();

        return new EdcHttpClientImpl(
                okHttpClient.newBuilder().dns(dns).build(),
                retryPolicy);
    }
}
