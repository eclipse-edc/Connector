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
        // prepare
        when(vaultClient.getSecretValue(KEY)).thenReturn(Result.success("test-secret"));

        // invoke
        var returnValue = vault.resolveSecret(KEY);

        // verify
        verify(vaultClient, times(1)).getSecretValue(KEY);
        assertThat(returnValue).isEqualTo("test-secret");
    }

    @Test
    void getSecretFailure() {
        // prepare
        when(vaultClient.getSecretValue(KEY)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.resolveSecret(KEY);

        // verify
        verify(vaultClient, times(1)).getSecretValue(KEY);
        assertThat(returnValue).isNull();
    }

    @Test
    void setSecretSuccess() {
        // prepare
        var value = UUID.randomUUID().toString();
        when(vaultClient.setSecret(KEY, value)).thenReturn(Result.success(null));

        // invoke
        var returnValue = vault.storeSecret(KEY, value);

        // verify
        verify(vaultClient, times(1)).setSecret(KEY, value);
        assertThat(returnValue.succeeded()).isTrue();
    }

    @Test
    void setSecretFailure() {
        // prepare
        var value = UUID.randomUUID().toString();
        when(vaultClient.setSecret(KEY, value)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.storeSecret(KEY, value);

        // verify
        verify(vaultClient, times(1)).setSecret(KEY, value);
        assertThat(returnValue.failed()).isTrue();
    }

    @Test
    void destroySecretSuccess() {
        // prepare
        when(vaultClient.destroySecret(KEY)).thenReturn(Result.success());

        // invoke
        var returnValue = vault.deleteSecret(KEY);

        // verify
        verify(vaultClient, times(1)).destroySecret(KEY);
        assertThat(returnValue.succeeded()).isTrue();
    }

    @Test
    void destroySecretFailure() {
        // prepare
        when(vaultClient.destroySecret(KEY)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.deleteSecret(KEY);

        // verify
        verify(vaultClient, times(1)).destroySecret(KEY);
        assertThat(returnValue.failed()).isTrue();
    }
}
