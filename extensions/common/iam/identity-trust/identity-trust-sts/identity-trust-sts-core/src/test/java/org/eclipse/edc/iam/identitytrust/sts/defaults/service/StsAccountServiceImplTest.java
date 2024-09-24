/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults.service;

import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StsAccountServiceImplTest {

    private final StsAccountStore store = mock();
    private final Vault vault = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private StsAccountServiceImpl clientService;

    @BeforeEach
    void setup() {
        clientService = new StsAccountServiceImpl(store, vault, transactionContext);
    }

    @Test
    void create() {
        var client = createClient("clientId");
        when(store.create(client)).thenReturn(StoreResult.success(client));

        var inserted = clientService.create(client);

        assertThat(inserted).isSucceeded();
        verify(store).create(client);
        verifyNoMoreInteractions(store);
    }

    @Test
    void findById() {
        var clientId = "clientId";
        var client = createClient(clientId);

        when(store.findByClientId(clientId)).thenReturn(StoreResult.success(client));

        var inserted = clientService.findByClientId(clientId);

        assertThat(inserted).isSucceeded().isEqualTo(client);
        verify(store).findByClientId(clientId);
        verifyNoMoreInteractions(store);
    }

    @Test
    void authenticate() {
        var clientId = "clientId";
        var secret = "secret";
        var client = createClient(clientId);
        when(vault.resolveSecret(client.getSecretAlias())).thenReturn(secret);

        var inserted = clientService.authenticate(client, secret);

        assertThat(inserted).isSucceeded();
        verify(vault).resolveSecret(client.getSecretAlias());
    }

}
