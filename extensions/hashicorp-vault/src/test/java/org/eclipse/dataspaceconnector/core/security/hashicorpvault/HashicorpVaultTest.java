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

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultTest {
    private static final String KEY = "key";

    private HashicorpVaultClient vaultClient;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vaultClient = mock(HashicorpVaultClient.class);
        var monitor = mock(Monitor.class);
        vault = new HashicorpVault(vaultClient, monitor);
    }

    @Test
    void getSecret() {
        var value = UUID.randomUUID().toString();
        var result = Result.success(value);
        when(vaultClient.getSecretValue(KEY)).thenReturn(result);

        var returnValue = vault.resolveSecret(KEY);

        verify(vaultClient, times(1)).getSecretValue(KEY);
        Assertions.assertEquals(value, returnValue);
    }

    @Test
    void setSecret() {
        var value = UUID.randomUUID().toString();
        var result = Result.success(mock(HashicorpVaultCreateEntryResponsePayload.class));
        when(vaultClient.setSecret(KEY, value)).thenReturn(result);

        var returnValue = vault.storeSecret(KEY, value);

        verify(vaultClient, times(1)).setSecret(KEY, value);
        Assertions.assertTrue(returnValue.succeeded());
    }

    @Test
    void destroySecret() {
        var result = Result.success();
        when(vaultClient.destroySecret(KEY)).thenReturn(result);

        var returnValue = vault.deleteSecret(KEY);

        verify(vaultClient, times(1)).destroySecret(KEY);
        Assertions.assertTrue(returnValue.succeeded());
    }
}
