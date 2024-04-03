/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.vault.hashicorp.client.HashicorpVaultClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultTest {
    private static final String KEY = "key";

    // mocks
    private HashicorpVaultClient vaultClient;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vaultClient = mock();
        var monitor = mock(Monitor.class);
        vault = new HashicorpVault(vaultClient, monitor);
    }

    @Test
    void getSecretSuccess() {
        when(vaultClient.getSecretValue(KEY)).thenReturn(Result.success("test-secret"));

        var returnValue = vault.resolveSecret(KEY);

        verify(vaultClient, times(1)).getSecretValue(KEY);
        assertThat(returnValue).isEqualTo("test-secret");
    }

    @Test
    void getSecretFailure() {
        when(vaultClient.getSecretValue(KEY)).thenReturn(Result.failure("test-failure"));

        var returnValue = vault.resolveSecret(KEY);

        verify(vaultClient, times(1)).getSecretValue(KEY);
        assertThat(returnValue).isNull();
    }

    @Test
    void setSecretSuccess() {
        var value = UUID.randomUUID().toString();
        when(vaultClient.setSecret(KEY, value)).thenReturn(Result.success(null));

        var returnValue = vault.storeSecret(KEY, value);

        verify(vaultClient, times(1)).setSecret(KEY, value);
        assertThat(returnValue.succeeded()).isTrue();
    }

    @Test
    void setSecretFailure() {
        var value = UUID.randomUUID().toString();
        when(vaultClient.setSecret(KEY, value)).thenReturn(Result.failure("test-failure"));

        var returnValue = vault.storeSecret(KEY, value);

        verify(vaultClient, times(1)).setSecret(KEY, value);
        assertThat(returnValue.failed()).isTrue();
    }

    @Test
    void destroySecretSuccess() {
        when(vaultClient.destroySecret(KEY)).thenReturn(Result.success());

        var returnValue = vault.deleteSecret(KEY);

        verify(vaultClient, times(1)).destroySecret(KEY);
        assertThat(returnValue.succeeded()).isTrue();
    }

    @Test
    void destroySecretFailure() {
        when(vaultClient.destroySecret(KEY)).thenReturn(Result.failure("test-failure"));

        var returnValue = vault.deleteSecret(KEY);

        verify(vaultClient, times(1)).destroySecret(KEY);
        assertThat(returnValue.failed()).isTrue();
    }
}
