/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
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

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This task implements the Hashicorp Vault token renewal mechanism.
 * To ensure that this task is really cancelled, call the stop method before program shut down.
 */
public class HashicorpVaultTokenRenewTask {

    private static final String INITIAL_TOKEN_LOOK_UP_ERR_MSG_FORMAT = "Initial token look up failed with reason: %s";
    private static final String INITIAL_TOKEN_RENEW_ERR_MSG_FORMAT = "Initial token renewal failed with reason: %s";
    private static final String SCHEDULED_TOKEN_RENEWAL_ERR_MSG_FORMAT = "Scheduled token renewal failed: %s";
    private static final String DATA_KEY = "data";
    private static final String RENEWABLE_KEY = "renewable";
    private static final String AUTH_KEY = "auth";
    private static final String LEASE_DURATION_KEY = "lease_duration";

    @NotNull
    private final ScheduledExecutorService scheduledExecutorService;
    @NotNull
    private final HashicorpVaultClient hashicorpVaultClient;
    @NotNull
    private final Monitor monitor;
    private final long renewBuffer;
    private final ObjectMapper objectMapper;
    private Future<?> tokenRenewTask;

    public HashicorpVaultTokenRenewTask(@NotNull ExecutorInstrumentation executorInstrumentation,
                                        @NotNull HashicorpVaultClient hashicorpVaultClient,
                                        @NotNull ObjectMapper objectMapper,
                                        long renewBuffer,
                                        @NotNull Monitor monitor) {
        this.scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), HashicorpVaultExtension.NAME);
        this.hashicorpVaultClient = hashicorpVaultClient;
        this.objectMapper = objectMapper;
        this.renewBuffer = renewBuffer;
        this.monitor = monitor;
    }

    /**
     * Starts the scheduled token renewal.
     */
    public void start() {
        initTokenRenewalTask();
    }

    /**
     * Stops the scheduled token renewal. Running tasks will be interrupted.
     */
    public void stop() {
        if (tokenRenewTask != null) {
            tokenRenewTask.cancel(true);
        }
        scheduledExecutorService.shutdownNow();
    }

    /**
     * Performs the initial token lookup and renewal. Schedules the token renewal if both operations were successful.
     * Runs asynchronously.
     */
    private void initTokenRenewalTask() {
        scheduledExecutorService.execute(() -> {
            var tokenLookUpResult = hashicorpVaultClient.lookUpToken();

            if (tokenLookUpResult.failed()) {
                monitor.warning(INITIAL_TOKEN_LOOK_UP_ERR_MSG_FORMAT.formatted(tokenLookUpResult.getFailureDetail()));
                return;
            }

            var tokenLookUpResponse = tokenLookUpResult.getContent();
            var parseRenewableResult = parseRenewable(tokenLookUpResponse);

            if (parseRenewableResult.failed()) {
                monitor.warning(INITIAL_TOKEN_LOOK_UP_ERR_MSG_FORMAT.formatted(parseRenewableResult.getFailureDetail()));
                return;
            }

            var isRenewable = parseRenewableResult.getContent();

            if (isRenewable) {
                var tokenRenewResult = hashicorpVaultClient.renewToken();

                if (tokenRenewResult.failed()) {
                    monitor.warning(INITIAL_TOKEN_RENEW_ERR_MSG_FORMAT.formatted(tokenRenewResult.getFailureDetail()));
                    return;
                }

                var tokenRenewResponse = tokenRenewResult.getContent();
                var parseTtlResult = parseTtl(tokenRenewResponse);

                if (parseTtlResult.failed()) {
                    monitor.warning(INITIAL_TOKEN_RENEW_ERR_MSG_FORMAT.formatted(parseTtlResult.getFailureDetail()));
                    return;
                }

                scheduleNextTokenRenewal(parseTtlResult.getContent());
            }
        });
    }

    /**
     * Schedules the token renewal which executes after a delay defined as {@code delay = ttl - renewBuffer}.
     * After successfully renewing the token the next renewal is scheduled. This operation will not be re-scheduled if
     * the renewal failed for some reason since tokens are invalidated forever after their ttl expires.
     *
     * @param ttl the ttl of the token
     */
    private void scheduleNextTokenRenewal(long ttl) {
        var delay = ttl - renewBuffer;

        tokenRenewTask = scheduledExecutorService.schedule(() -> {
            var tokenRenewResult = hashicorpVaultClient.renewToken();

            if (tokenRenewResult.failed()) {
                monitor.warning(SCHEDULED_TOKEN_RENEWAL_ERR_MSG_FORMAT.formatted(tokenRenewResult.getFailureDetail()));
                return;
            }

            var tokenRenewResponse = tokenRenewResult.getContent();
            var parseTtlResult = parseTtl(tokenRenewResponse);

            if (parseTtlResult.failed()) {
                monitor.warning(SCHEDULED_TOKEN_RENEWAL_ERR_MSG_FORMAT.formatted(parseTtlResult.getFailureDetail()));
                return;
            }

            scheduleNextTokenRenewal(parseTtlResult.getContent());
        }, delay, TimeUnit.SECONDS);
    }

    private Result<Boolean> parseRenewable(Map<String, Object> map) {
        try {
            var data = objectMapper.convertValue(getValueFromMap(map, DATA_KEY), new TypeReference<Map<String, Object>>() {});
            var isRenewable = objectMapper.convertValue(getValueFromMap(data, RENEWABLE_KEY), Boolean.class);
            return Result.success(isRenewable);
        } catch (IllegalArgumentException e) {
            var errMsg = "Failed to parse renewable flag from %s with reason: %s".formatted(map, e.getMessage());
            monitor.warning(errMsg);
            return Result.failure(errMsg);
        }
    }

    private Result<Long> parseTtl(Map<String, Object> map) {
        try {
            var auth = objectMapper.convertValue(getValueFromMap(map, AUTH_KEY), new TypeReference<Map<String, Object>>() {});
            var ttl = objectMapper.convertValue(getValueFromMap(auth, LEASE_DURATION_KEY), Long.class);
            return Result.success(ttl);
        } catch (IllegalArgumentException e) {
            var errMsg = "Failed to parse ttl from %s with reason: %s".formatted(map, e.getMessage());
            monitor.warning(errMsg);
            return Result.failure(errMsg);
        }
    }

    private Object getValueFromMap(Map<String, Object> map, String key) {
        var value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Key '%s' does not exist".formatted(key));
        }
        return value;
    }
}
