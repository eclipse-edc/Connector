/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.http.client.testfixtures;

import dev.failsafe.RetryPolicy;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

/**
 * HTTP-related test utilities.
 */
public class HttpTestUtils {

    /**
     * Create an {@link OkHttpClient} suitable for using in unit tests. The client configured with long timeouts
     * suitable for high-contention scenarios in CI.
     *
     * @return an {@link OkHttpClient.Builder}.
     */
    public static OkHttpClient testOkHttpClient(Interceptor... interceptors) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES);

        for (Interceptor interceptor : interceptors) {
            builder.addInterceptor(interceptor);
        }

        return builder.build();
    }

    /**
     * Create an {@link EdcHttpClient} suitable for using in unit tests. The client configured with long timeouts
     * suitable for high-contention scenarios in CI.
     *
     * @return an {@link OkHttpClient.Builder}.
     */
    public static EdcHttpClient testHttpClient(Interceptor... interceptors) {
        return new EdcHttpClientImpl(testOkHttpClient(interceptors), RetryPolicy.ofDefaults(), Mockito.mock(Monitor.class));
    }

    private HttpTestUtils() {
    }
}
