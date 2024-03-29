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

import dev.failsafe.Fallback;
import dev.failsafe.event.ExecutionAttemptedEvent;
import dev.failsafe.function.CheckedFunction;
import okhttp3.Response;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * A set of global defined {@link FallbackFactory} that can be used with {@link EdcHttpClient}
 */
public interface FallbackFactories {

    /**
     * Verifies that the response code is between 400 and 499, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory retryWhenStatusNot2xxOr4xx() {
        return request -> {
            CheckedFunction<ExecutionAttemptedEvent<? extends Response>, Exception> exceptionSupplier = event -> {
                var response = event.getLastResult();
                if (response == null) {
                    return new EdcHttpClientException(event.getLastException().getMessage());
                } else {
                    return new EdcHttpClientException(format("Server response to [%s, %s] was not successful but was %s: %s", request.method(), request.url(), response.code(), response.body().string()));
                }
            };
            return Fallback.builderOfException(exceptionSupplier)
                    .handleResultIf(r -> !(r.isSuccessful() || r.code() >= 400 && r.code() < 500))
                    .build();
        };
    }

    /**
     * Verifies that the response has a specific status, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory retryWhenStatusIsNot(int status) {
        return retryWhenStatusIsNotIn(status);
    }

    /**
     * Verifies that the response has a specific statuses, otherwise it should be retried
     *
     * @return the {@link FallbackFactory}
     */
    static FallbackFactory retryWhenStatusIsNotIn(int... status) {
        var codes = Arrays.stream(status).boxed().collect(Collectors.toSet());
        return request -> {
            CheckedFunction<ExecutionAttemptedEvent<? extends Response>, Exception> exceptionSupplier = event -> {
                var response = event.getLastResult();
                if (response == null) {
                    return new EdcHttpClientException(event.getLastException().getMessage());
                } else {
                    return new EdcHttpClientException(format("Server response to [%s, %s] was not one of %s but was %s: %s", request.method(), request.url(), Arrays.toString(status), response.code(), response.body().string()));
                }
            };
            return Fallback.builderOfException(exceptionSupplier)
                    .handleResultIf(r -> !codes.contains(r.code()))
                    .build();
        };
    }
}
