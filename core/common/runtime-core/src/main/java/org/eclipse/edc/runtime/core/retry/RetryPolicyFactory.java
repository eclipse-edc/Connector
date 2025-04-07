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

package org.eclipse.edc.runtime.core.retry;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Factory for Failsafe's {@link RetryPolicy}
 */
public class RetryPolicyFactory {

    /**
     * Create a RetryPolicy given the configuration.
     *
     * @param configuration the configuration.
     * @param monitor the monitor.
     * @return the RetryPolicy.
     */
    public static <T> RetryPolicy<T> create(RetryPolicyConfiguration configuration, Monitor monitor) {
        var builder = RetryPolicy.<T>builder()
                .withMaxRetries(configuration.getMaxRetries())
                .withBackoff(configuration.getMinBackoff(), configuration.getMaxBackoff(), MILLIS);

        if (configuration.isLogOnRetry()) {
            builder.onRetry(event -> monitor
                    .debug("Failsafe: execution attempted, will retry.", event.getLastException()));
        }

        if (configuration.isLogOnRetryScheduled()) {
            builder.onRetryScheduled(event -> monitor
                    .debug(format("Failsafe: execution scheduled, will retry in %s.", event.getDelay()), event.getLastException()));
        }

        if (configuration.isLogOnRetriesExceeded()) {
            builder.onRetriesExceeded(event -> monitor
                    .debug("Failsafe: maximum retries exceeded", event.getException()));
        }

        if (configuration.isLogOnFailedAttempt()) {
            builder.onFailedAttempt(event -> monitor
                    .debug("Failsafe: execution attempt failed", event.getLastException()));
        }

        if (configuration.isLogOnAbort()) {
            builder.onAbort(event -> monitor
                    .debug("Failsafe: execution aborted", event.getException()));
        }

        return builder.build();
    }
}
