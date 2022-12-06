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

    private HashicorpVaultClient vaultHashicorpVaultClient;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vaultHashicorpVaultClient = mock(HashicorpVaultClient.class);
        var monitor = mock(Monitor.class);
        vault = new HashicorpVault(vaultHashicorpVaultClient, monitor);
    }

    @Test
    void getSecret() {
        var value = UUID.randomUUID().toString();
        var result = Result.success(value);
        when(vaultHashicorpVaultClient.getSecretValue(KEY)).thenReturn(result);

        var returnValue = vault.resolveSecret(KEY);

        verify(vaultHashicorpVaultClient, times(1)).getSecretValue(KEY);
        assertThat(returnValue).isEqualTo(value);
    }

    @Test
    void setSecret() {
        var value = UUID.randomUUID().toString();
        var result = Result.success(mock(CreateEntryResponsePayload.class));
        when(vaultHashicorpVaultClient.setSecret(KEY, value)).thenReturn(result);

        var returnValue = vault.storeSecret(KEY, value);

        verify(vaultHashicorpVaultClient, times(1)).setSecret(KEY, value);
        assertThat(returnValue.succeeded()).isTrue();
    }

    @Test
    void destroySecret() {
        var result = Result.success();
        when(vaultHashicorpVaultClient.destroySecret(KEY)).thenReturn(result);

        var returnValue = vault.deleteSecret(KEY);

        verify(vaultHashicorpVaultClient, times(1)).destroySecret(KEY);
        assertThat(returnValue.succeeded()).isTrue();
    }
}
