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
import org.mockito.Mockito;

import java.util.UUID;

class HashicorpVaultTest {
    private static final String KEY = "key";

    // mocks
    private HashicorpVaultClient vaultClient;
    private HashicorpVault vault;

    @BeforeEach
    void setup() {
        vaultClient = Mockito.mock(HashicorpVaultClient.class);
        final Monitor monitor = Mockito.mock(Monitor.class);
        vault = new HashicorpVault(vaultClient, monitor);
    }

    @Test
    void getSecret() {
        // prepare
        var value = UUID.randomUUID().toString();
        var result = Mockito.mock(Result.class);
        Mockito.when(vaultClient.getSecretValue(KEY)).thenReturn(result);
        Mockito.when(result.getContent()).thenReturn(value);
        Mockito.when(result.succeeded()).thenReturn(true);

        // invoke
        var returnValue = vault.resolveSecret(KEY);

        // verify
        Mockito.verify(vaultClient, Mockito.times(1)).getSecretValue(KEY);
        Assertions.assertEquals(value, returnValue);
    }

    @Test
    void setSecret() {
        // prepare
        var value = UUID.randomUUID().toString();
        var result = Mockito.mock(Result.class);
        Mockito.when(vaultClient.setSecret(KEY, value)).thenReturn(result);
        Mockito.when(result.succeeded()).thenReturn(true);

        // invoke
        var returnValue = vault.storeSecret(KEY, value);

        // verify
        Mockito.verify(vaultClient, Mockito.times(1)).setSecret(KEY, value);
        Assertions.assertTrue(returnValue.succeeded());
    }

    @Test
    void destroySecret() {
        // prepare
        var result = Mockito.mock(Result.class);
        Mockito.when(vaultClient.destroySecret(KEY)).thenReturn(result);
        Mockito.when(result.succeeded()).thenReturn(true);

        // invoke
        var returnValue = vault.deleteSecret(KEY);

        // verify
        Mockito.verify(vaultClient, Mockito.times(1)).destroySecret(KEY);
        Assertions.assertTrue(returnValue.succeeded());
    }
}
