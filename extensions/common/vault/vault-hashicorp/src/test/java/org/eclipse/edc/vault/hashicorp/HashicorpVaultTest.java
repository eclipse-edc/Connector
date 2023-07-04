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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class HashicorpVaultTest {
    private static final String KEY = "key";

    // mocks
    private HashicorpVaultClient vaultClient;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vaultClient = Mockito.mock(HashicorpVaultClient.class);
        var monitor = Mockito.mock(Monitor.class);
        vault = new HashicorpVault(vaultClient, monitor);
    }

    @Test
    void getSecretSuccess() {
        // prepare
        Mockito.when(vaultClient.getSecretValue(KEY)).thenReturn(Result.success("test-secret"));

        // invoke
        var returnValue = vault.resolveSecret(KEY);

        // verify
        verify(vaultClient, Mockito.times(1)).getSecretValue(KEY);
        assertThat(returnValue).isEqualTo("test-secret");
    }

    @Test
    void getSecretFailure() {
        // prepare
        Mockito.when(vaultClient.getSecretValue(KEY)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.resolveSecret(KEY);

        // verify
        verify(vaultClient, Mockito.times(1)).getSecretValue(KEY);
        assertThat(returnValue).isNull();
    }

    @Test
    void setSecretSuccess() {
        // prepare
        var value = UUID.randomUUID().toString();
        Mockito.when(vaultClient.setSecret(KEY, value)).thenReturn(Result.success(null));

        // invoke
        var returnValue = vault.storeSecret(KEY, value);

        // verify
        verify(vaultClient, Mockito.times(1)).setSecret(KEY, value);
        Assertions.assertTrue(returnValue.succeeded());
    }

    @Test
    void setSecretFailure() {
        // prepare
        var value = UUID.randomUUID().toString();
        Mockito.when(vaultClient.setSecret(KEY, value)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.storeSecret(KEY, value);

        // verify
        verify(vaultClient, Mockito.times(1)).setSecret(KEY, value);
        Assertions.assertTrue(returnValue.failed());
    }

    @Test
    void destroySecretSuccess() {
        // prepare
        Mockito.when(vaultClient.destroySecret(KEY)).thenReturn(Result.success());

        // invoke
        var returnValue = vault.deleteSecret(KEY);

        // verify
        verify(vaultClient, Mockito.times(1)).destroySecret(KEY);
        Assertions.assertTrue(returnValue.succeeded());
    }

    @Test
    void destroySecretFailure() {
        // prepare
        Mockito.when(vaultClient.destroySecret(KEY)).thenReturn(Result.failure("test-failure"));

        // invoke
        var returnValue = vault.deleteSecret(KEY);

        // verify
        verify(vaultClient, Mockito.times(1)).destroySecret(KEY);
        Assertions.assertTrue(returnValue.failed());
    }
}
