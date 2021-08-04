/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.system;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;

import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CoreServicesExtension implements ServiceExtension {

    public static final String FEATURE_HTTP_CLIENT = "dataspaceconnector:http-client";
    public static final String FEATURE_RETRY_POLICY = "dataspaceconnector:retry-policy";
    private static final String MAX_RETRIES = "dataspaceconnector:core.retry.max-retries";
    private static final String BACKOFF_MIN_MILLIS = "dataspaceconnector.core.retry.backoff.min";
    private static final String BACKOFF_MAX_MILLIS = "dataspaceconnector.core.retry.backoff.max";

    @Override
    public Set<String> provides() {
        return Set.of(FEATURE_HTTP_CLIENT, FEATURE_RETRY_POLICY);
    }

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        Monitor monitor = context.getMonitor();
        addHttpClient(context);
        addRetryPolicy(context);
        monitor.info("Initialized Core Services extension.");
    }


    private void addRetryPolicy(ServiceExtensionContext context) {

        var maxRetries = Integer.parseInt(context.getSetting(MAX_RETRIES, String.valueOf(5)));
        var minBackoff = Integer.parseInt(context.getSetting(BACKOFF_MIN_MILLIS, String.valueOf(500)));
        var maxBackoff = Integer.parseInt(context.getSetting(BACKOFF_MAX_MILLIS, String.valueOf(10_000)));

        var retryPolicy = new RetryPolicy<>()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS);

        context.registerService(RetryPolicy.class, retryPolicy);

    }

    private void addHttpClient(ServiceExtensionContext context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS);
//        if (interceptors != null) {
//            for (Interceptor interceptor : interceptors) {
//                builder.addInterceptor(interceptor);
//            }
//        }
        var client = builder.build();

        context.registerService(OkHttpClient.class, client);
    }
}
