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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class HashicorpVaultTokenRenewTaskTest {
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 5L;
    private static final String DATA_KEY = "data";
    private static final String RENEWABLE_KEY = "renewable";
    private static final String AUTH_KEY = "auth";
    private static final String LEASE_DURATION_KEY = "lease_duration";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Monitor monitor = mock();
    private final HashicorpVaultClient client = mock();
    private final HashicorpVaultTokenRenewTask tokenRenewTask = new HashicorpVaultTokenRenewTask(
            ExecutorInstrumentation.noop(),
            client,
            OBJECT_MAPPER,
            RENEW_BUFFER,
            monitor
    );

    @Test
    void start_withValidAndRenewableToken_shouldScheduleNextTokenRenewal() {
        var tokenLookUpResponse = Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, true));
        doReturn(Result.success(tokenLookUpResponse)).when(client).lookUpToken();
        var tokenRenewResponse = Map.of(AUTH_KEY, Map.of(LEASE_DURATION_KEY, 2L));
        // return a successful renewal result twice
        // first result should be consumed by the initial token renewal
        // second renewal should be consumed by the first renewal iteration
        doReturn(Result.success(tokenRenewResponse))
                .doReturn(Result.success(tokenRenewResponse))
                // break the renewal loop by returning a failed renewal result on the 3rd attempt
                .doReturn(Result.failure("break the loop"))
                .when(client)
                .renewToken();

        tokenRenewTask.start();

        await()
                .atMost(VAULT_TOKEN_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor, never()).warning(matches("Initial token look up failed with reason: *"));
                    verify(monitor, never()).warning(matches("Initial token renewal failed with reason: *"));
                    // initial token look up
                    verify(client).lookUpToken();
                    // initial renewal + first scheduled renewal + second scheduled renewal
                    verify(client, times(3)).renewToken();
                    verify(monitor).warning("Scheduled token renewal failed: break the loop");
                });
    }

    @Test
    void start_withFailedTokenLookUp_shouldNotScheduleNextTokenRenewal() {
        doReturn(Result.failure("Token look up failed with status 403")).when(client).lookUpToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning("Initial token look up failed with reason: Token look up failed with status 403");
                    verify(client, never()).renewToken();
                });
    }

    @Test
    void start_withTokenNotRenewable_shouldNotScheduleNextTokenRenewal() {
        var tokenLookUpResponse = Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, false));
        doReturn(Result.success(tokenLookUpResponse)).when(client).lookUpToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor, never()).warning(anyString());
                    verify(client, never()).renewToken();
                });
    }

    @Test
    void start_withFailedTokenRenew_shouldNotScheduleNextTokenRenewal() {
        var tokenLookUpResponse = Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, true));
        doReturn(Result.success(tokenLookUpResponse)).when(client).lookUpToken();
        doReturn(Result.failure("Token renew failed with status: 403")).when(client).renewToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning("Initial token renewal failed with reason: Token renew failed with status: 403");
                    verify(client, atMostOnce()).renewToken();
                });
    }

    @ParameterizedTest
    @MethodSource("invalidTokenLookUpResponseProvider")
    void start_withInvalidTokenLookUpResponse_shouldNotScheduleNextTokenRenewal(Map<String, Object> tokenLookUpResponse) {
        doReturn(Result.success(tokenLookUpResponse)).when(client).lookUpToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning(matches("Initial token look up failed with reason: Failed to parse renewable flag from *"));
                    verify(client, atMostOnce()).renewToken();
                });
    }

    @ParameterizedTest
    @MethodSource("invalidTokenRenewResponseProvider")
    void start_withInvalidTokenRenewResponse_shouldNotScheduleNextTokenRenewal(Map<String, Object> tokenRenewResponse) {
        var tokenLookUpResponse = Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, true));
        doReturn(Result.success(tokenLookUpResponse)).when(client).lookUpToken();
        doReturn(Result.success(tokenRenewResponse)).when(client).renewToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning(matches("Initial token renewal failed with reason: Failed to parse ttl from *"));
                    verify(client, atMostOnce()).renewToken();
                });
    }

    private static List<Map<String, Object>> invalidTokenLookUpResponseProvider() {
        return List.of(
                Map.of(),
                Map.of(DATA_KEY, Map.of()),
                Map.of(DATA_KEY, Map.of(RENEWABLE_KEY, "not a boolean"))
        );
    }

    private static List<Map<String, Object>> invalidTokenRenewResponseProvider() {
        return List.of(
                Map.of(),
                Map.of(AUTH_KEY, Map.of()),
                Map.of(AUTH_KEY, Map.of(LEASE_DURATION_KEY, "not a long"))
        );
    }
}
