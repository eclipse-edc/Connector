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
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StsAccountServiceImplTest {

    private final StsAccountStore store = mock();
    private final Vault vault = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private StsAccountServiceImpl clientService;

    @BeforeEach
    void setup() {
        clientService = new StsAccountServiceImpl(store, vault, transactionContext, parameters -> UUID.randomUUID().toString());
    }

    @Test
    void create() {
        var client = createClient("clientId");
        when(store.create(client)).thenReturn(StoreResult.success(client));
        when(vault.storeSecret(any(), any())).thenReturn(Result.success());

        var inserted = clientService.create(client, "some-secret");

        assertThat(inserted).isSucceeded();
        verify(store).create(client);
        verifyNoMoreInteractions(store);
    }

    @Test
    void create_whenAlreadyExists() {
        when(store.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var client = createClient("clientId");
        assertThat(clientService.create(client, "some-secret"))
                .isFailed()
                .detail().contains("foo");

        verifyNoMoreInteractions(vault);
    }

    @Test
    void findByClientId() {
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

    @Test
    void update() {
        when(store.update(any())).thenReturn(StoreResult.success());

        var client = createClient("clientId");
        assertThat(clientService.update(client)).isSucceeded();
        verify(store).update(client);
        verifyNoInteractions(vault);
    }

    @Test
    void update_whenNotExists() {
        when(store.update(any())).thenReturn(StoreResult.notFound("foo"));

        var client = createClient("clientId");
        assertThat(clientService.update(client)).isFailed()
                .detail().contains("foo");
        verify(store).update(client);
        verifyNoMoreInteractions(vault, store);
    }

    @Test
    void updateSecret() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(store.findById(any())).thenReturn(StoreResult.success(client));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.success());
        when(vault.storeSecret(eq("new-alias"), eq("new-secret"))).thenReturn(Result.success());

        assertThat(clientService.updateSecret(client.getId(), "new-alias", "new-secret")).isSucceeded();


        verify(store).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(vault).storeSecret("new-alias", "new-secret");
        verify(store).update(any());
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void updateSecret_aliasNull() {
        var client = createClient("clientId");

        assertThatThrownBy(() -> clientService.updateSecret(client.getId(), null, "some-secret"))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(store, vault);
    }

    @Test
    void updateSecret_secretNull() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(store.findById(any())).thenReturn(StoreResult.success(client));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.success());
        when(vault.storeSecret(eq("new-alias"), anyString())).thenReturn(Result.success());

        assertThat(clientService.updateSecret(client.getId(), "new-alias")).isSucceeded();

        verify(store).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(vault).storeSecret(eq("new-alias"), anyString());
        verify(store).update(any());
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void updateSecret_whenNotExists() {
        var client = createClient("clientId");
        when(store.findById(any())).thenReturn(StoreResult.notFound("foo"));

        assertThat(clientService.updateSecret(client.getId(), "new-alias")).isFailed().detail()
                .isEqualTo("foo");

        verify(store).findById(client.getId());
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void updateSecret_vaultFailsToDelete() {
        var client = createClient("clientId");
        var oldAlias = client.getSecretAlias();
        when(store.findById(any())).thenReturn(StoreResult.success(client));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(vault.deleteSecret(eq(oldAlias))).thenReturn(Result.failure("foo"));

        assertThat(clientService.updateSecret(client.getId(), "new-alias")).isFailed();

        verify(store).findById(client.getId());
        verify(vault).deleteSecret(oldAlias);
        verify(store).update(any());
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void deleteById() {
        when(store.deleteById(any())).thenReturn(StoreResult.success(createClient("test-id")));
        when(vault.deleteSecret(any())).thenReturn(Result.success());
        assertThat(clientService.deleteById("test-id")).isSucceeded();
        verify(store).deleteById("test-id");
        verify(vault).deleteSecret(any());
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void deleteById_whenNotExists() {
        when(store.deleteById(any())).thenReturn(StoreResult.notFound("foo"));
        assertThat(clientService.deleteById("test-id")).isFailed().detail().isEqualTo("foo");
        verify(store).deleteById("test-id");
        verifyNoMoreInteractions(store, vault);
    }

    @Test
    void query() {
        var id1 = createClient("id1");
        var id2 = createClient("id2");
        when(store.findAll(any())).thenReturn(Stream.of(id1, id2));

        assertThat(clientService.queryAccounts(QuerySpec.max()))
                .containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    void query_noResults() {
        when(store.findAll(any())).thenReturn(Stream.of());
        assertThat(clientService.queryAccounts(QuerySpec.max()))
                .isEmpty();
    }

    @Test
    void findById() {
        var client = createClient("test-id");
        when(store.findById(anyString())).thenReturn(StoreResult.success(client));
        assertThat(clientService.findById("test-id")).isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(client);
    }

    @Test
    void findById_whenNotExists() {
        when(store.findById(anyString())).thenReturn(StoreResult.notFound("foo"));
        assertThat(clientService.findById("test-id")).isFailed()
                .detail().isEqualTo("foo");
    }
}
