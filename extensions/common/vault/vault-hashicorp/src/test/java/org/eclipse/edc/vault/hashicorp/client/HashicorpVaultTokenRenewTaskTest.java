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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

class HashicorpVaultTokenRenewTaskTest {
    private static final long VAULT_TOKEN_TTL = 5L;
    private static final long RENEW_BUFFER = 5L;
    private final Monitor monitor = mock();
    private final HashicorpVaultHealthService client = mock();
    private final HashicorpVaultTokenRenewTask tokenRenewTask = new HashicorpVaultTokenRenewTask(
            "Hashicorp Vault",
            ExecutorInstrumentation.noop(),
            client,
            RENEW_BUFFER,
            monitor
    );

    @Test
    void start_withValidAndRenewableToken_shouldScheduleNextTokenRenewal() {
        doReturn(Result.success(Boolean.TRUE)).when(client).isTokenRenewable();
        // return a successful renewal result twice
        // first result should be consumed by the initial token renewal
        // second renewal should be consumed by the first renewal iteration
        doReturn(Result.success(VAULT_TOKEN_TTL))
                .doReturn(Result.success(VAULT_TOKEN_TTL))
                // break the renewal loop by returning a failed renewal result on the 3rd attempt
                .doReturn(Result.failure("break the loop"))
                .when(client)
                .renewToken();
        assertThat(tokenRenewTask.isRunning()).isFalse();

        tokenRenewTask.start();

        await()
                .atMost(VAULT_TOKEN_TTL, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor, never()).warning(matches("Initial token look up failed with reason: *"));
                    verify(monitor, never()).warning(matches("Initial token renewal failed with reason: *"));
                    // initial token look up
                    verify(client).isTokenRenewable();
                    // initial renewal + first scheduled renewal + second scheduled renewal
                    verify(client, times(3)).renewToken();
                    verify(monitor).warning("Scheduled token renewal failed: break the loop");
                });
        assertThat(tokenRenewTask.isRunning()).isTrue();
    }

    @Test
    void start_withFailedTokenLookUp_shouldNotScheduleNextTokenRenewal() {
        doReturn(Result.failure("Token look up failed with status 403")).when(client).isTokenRenewable();

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
        doReturn(Result.success(Boolean.FALSE)).when(client).isTokenRenewable();

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
        doReturn(Result.success(Boolean.TRUE)).when(client).isTokenRenewable();
        doReturn(Result.failure("Token renew failed with status: 403")).when(client).renewToken();

        tokenRenewTask.start();

        await()
                .atMost(1L, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(monitor).warning("Initial token renewal failed with reason: Token renew failed with status: 403");
                    verify(client, atMostOnce()).renewToken();
                });
    }

    @Test
    void stop_withTaskRunning_shouldSetRunningFalse() {
        tokenRenewTask.start();
        assertThat(tokenRenewTask.isRunning()).isTrue();
        tokenRenewTask.stop();
        assertThat(tokenRenewTask.isRunning()).isFalse();
    }
}
