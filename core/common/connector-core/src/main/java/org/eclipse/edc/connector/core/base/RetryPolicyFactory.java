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
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.time.temporal.ChronoUnit;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

/**
 * Factory for Failsafe's {@link RetryPolicy}
 */
public class RetryPolicyFactory {

    private static final String DEFAULT_RETRIES = "5";
    private static final String DEFAULT_MIN_BACKOFF = "500";
    private static final String DEFAULT_MAX_BACKOFF = "10000";
    private static final String DEFAULT_LOG = "false";

    @Setting(value = "Maximum retries for the retry policy before a failure is propagated", defaultValue = DEFAULT_RETRIES)
    private static final String MAX_RETRIES = "edc.core.retry.retries.max";
    @Setting(value = "Minimum number of milliseconds for exponential backoff", defaultValue = DEFAULT_MIN_BACKOFF)
    private static final String BACKOFF_MIN_MILLIS = "edc.core.retry.backoff.min";
    @Setting(value = "Maximum number of milliseconds for exponential backoff.", defaultValue = DEFAULT_MAX_BACKOFF)
    private static final String BACKOFF_MAX_MILLIS = "edc.core.retry.backoff.max";

    @Setting(value = "Log Failsafe onRetry events", defaultValue = DEFAULT_LOG)
    static final String LOG_ON_RETRY = "edc.core.retry.log.on.retry";
    @Setting(value = "Log Failsafe onRetryScheduled events", defaultValue = DEFAULT_LOG)
    static final String LOG_ON_RETRY_SCHEDULED = "edc.core.retry.log.on.retry.scheduled";
    @Setting(value = "Log Failsafe onRetriesExceeded events", defaultValue = DEFAULT_LOG)
    static final String LOG_ON_RETRIES_EXCEEDED = "edc.core.retry.log.on.retries.exceeded";
    @Setting(value = "Log Failsafe onFailedAttempt events", defaultValue = DEFAULT_LOG)
    static final String LOG_ON_FAILED_ATTEMPT = "edc.core.retry.log.on.failed.attempt";
    @Setting(value = "Log Failsafe onAbort events", defaultValue = DEFAULT_LOG)
    static final String LOG_ON_ABORT = "edc.core.retry.log.on.abort";

    /**
     * Create a {@link RetryPolicy} given the configuration.
     *
     * @param <T> retry policy type
     * @param context the service extension context
     * @return a RetryPolicy
     */
    public static <T> RetryPolicy<T> create(ServiceExtensionContext context) {
        var maxRetries = context.getSetting(MAX_RETRIES, parseInt(DEFAULT_RETRIES));
        var minBackoff = context.getSetting(BACKOFF_MIN_MILLIS, parseInt(DEFAULT_MIN_BACKOFF));
        var maxBackoff = context.getSetting(BACKOFF_MAX_MILLIS, parseInt(DEFAULT_MAX_BACKOFF));

        var builder = RetryPolicy.<T>builder()
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff, ChronoUnit.MILLIS);

        if (context.getSetting(LOG_ON_RETRY, false)) {
            builder.onRetry(event -> context.getMonitor()
                    .debug("Failsafe: execution attempted, will retry.", event.getLastException()));
        }

        if (context.getSetting(LOG_ON_RETRY_SCHEDULED, false)) {
            builder.onRetryScheduled(event -> context.getMonitor()
                    .debug(format("Failsafe: execution scheduled, will retry in %s.", event.getDelay()), event.getLastException()));
        }

        if (context.getSetting(LOG_ON_RETRIES_EXCEEDED, false)) {
            builder.onRetriesExceeded(event -> context.getMonitor()
                    .debug("Failsafe: maximum retries exceeded", event.getException()));
        }

        if (context.getSetting(LOG_ON_FAILED_ATTEMPT, false)) {
            builder.onFailedAttempt(event -> context.getMonitor()
                    .debug("Failsafe: execution attempt failed", event.getLastException()));
        }

        if (context.getSetting(LOG_ON_ABORT, false)) {
            builder.onAbort(event -> context.getMonitor()
                    .debug("Failsafe: execution aborted", event.getException()));
        }

        return builder.build();
    }
}
