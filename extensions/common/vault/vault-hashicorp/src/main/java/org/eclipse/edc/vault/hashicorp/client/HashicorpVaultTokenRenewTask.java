/*
 *  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This task implements the Hashicorp Vault token renewal mechanism.
 * To ensure that this task is really cancelled, call the stop method before program shut down.
 */
public class HashicorpVaultTokenRenewTask {

    private static final String INITIAL_TOKEN_LOOK_UP_ERR_MSG_FORMAT = "Initial token look up failed with reason: %s";
    private static final String INITIAL_TOKEN_RENEW_ERR_MSG_FORMAT = "Initial token renewal failed with reason: %s";
    private static final String SCHEDULED_TOKEN_RENEWAL_ERR_MSG_FORMAT = "Scheduled token renewal failed: %s";

    private final String name;

    private final ExecutorInstrumentation executorInstrumentation;

    private final HashicorpVaultHealthService client;

    private final Monitor monitor;
    private final long renewBuffer;
    private ScheduledExecutorService scheduledExecutorService;
    private Future<?> tokenRenewTask;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Constructor for the HashicorpVaultTokenRenewTask.
     * Pass a reasonable {@code renewBuffer} (like 10s) to ensure that the token renewal operation can be executed
     * before the token expires and failed renewals can be retried in time.
     *
     * @param executorInstrumentation executor instrumentation used to initialize a {@link ScheduledExecutorService}
     * @param client                  the HashicorpVaultClient
     * @param renewBuffer             the renewal buffer time in seconds
     * @param monitor                 the monitor
     */
    public HashicorpVaultTokenRenewTask(@NotNull String name,
                                        @NotNull ExecutorInstrumentation executorInstrumentation,
                                        @NotNull HashicorpVaultHealthService client,
                                        long renewBuffer,
                                        @NotNull Monitor monitor) {
        this.name = name;
        this.executorInstrumentation = executorInstrumentation;
        this.client = client;
        this.renewBuffer = renewBuffer;
        this.monitor = monitor;
    }

    /**
     * Starts the scheduled token renewal.
     * Runs asynchronously.
     */
    public void start() {
        if (!isRunning()) {
            scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), name);
            scheduledExecutorService.execute(this::initialize);
            isRunning.set(true);
        }
    }

    /**
     * Stops the scheduled token renewal. Running tasks will be interrupted.
     */
    public void stop() {
        if (isRunning()) {
            if (tokenRenewTask != null) {
                tokenRenewTask.cancel(true);
            }
            scheduledExecutorService.shutdownNow();
            isRunning.set(false);
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void initialize() {
        var tokenLookUpResult = client.isTokenRenewable();

        if (tokenLookUpResult.succeeded()) {
            var isRenewable = tokenLookUpResult.getContent();

            if (isRenewable) {
                renewToken(INITIAL_TOKEN_RENEW_ERR_MSG_FORMAT);
            }
        } else {
            monitor.warning(INITIAL_TOKEN_LOOK_UP_ERR_MSG_FORMAT.formatted(tokenLookUpResult.getFailureDetail()));
        }
    }

    /**
     * Renews the token & schedules the next renewal if successful. The renewal is not scheduled in error cases since
     * tokens are invalidated forever after their ttl expires.
     *
     * @param errMsgFormat the error message format
     */
    private void renewToken(String errMsgFormat) {
        var tokenRenewResult = client.renewToken();

        if (tokenRenewResult.succeeded()) {
            var ttl = tokenRenewResult.getContent();
            scheduleNextTokenRenewal(ttl);
        } else {
            monitor.warning(errMsgFormat.formatted(tokenRenewResult.getFailureDetail()));
        }
    }

    /**
     * Schedules the token renewal operation which executes after a delay defined as {@code delay = ttl - renewBuffer}.
     *
     * @param ttl the ttl of the token
     */
    private void scheduleNextTokenRenewal(long ttl) {
        var delay = ttl - renewBuffer;

        tokenRenewTask = scheduledExecutorService.schedule(
                () -> renewToken(SCHEDULED_TOKEN_RENEWAL_ERR_MSG_FORMAT),
                delay,
                TimeUnit.SECONDS);
    }
}
