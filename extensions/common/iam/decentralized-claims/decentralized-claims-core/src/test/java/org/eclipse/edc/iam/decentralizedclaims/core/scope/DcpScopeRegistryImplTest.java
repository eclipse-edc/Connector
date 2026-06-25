/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.core.scope;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DcpScopeRegistryImplTest {


    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final DcpScopeStore store = mock();

    @Test
    void register() {
        var scope = DcpScope.Builder.newInstance()
                .id("s1")
                .value("v")
                .profile("p")
                .build();

        when(store.save(scope)).thenReturn(StoreResult.success());

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.register(scope);

        assertThat(res).isSucceeded();

        verify(store).save(scope);
    }

    @Test
    void register_should_return_failure_when_store_fails() {
        var scope = DcpScope.Builder.newInstance()
                .id("s2")
                .value("v")
                .profile("p")
                .build();

        when(store.save(scope)).thenReturn(StoreResult.generalError("boom"));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.register(scope);

        assertThat(res).isFailed().detail().contains("boom");

        verify(store).save(scope);
    }

    @Test
    void create_should_save_when_scope_does_not_exist() {
        var scope = DcpScope.Builder.newInstance().id("s1").value("v").profile("p").build();
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
        when(store.save(scope)).thenReturn(StoreResult.success());

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.create(scope);

        assertThat(res).isSucceeded();
        verify(store).save(scope);
    }

    @Test
    void create_should_return_conflict_when_scope_already_exists() {
        var scope = DcpScope.Builder.newInstance().id("s1").value("v").profile("p").build();
        when(store.query(any())).thenReturn(StoreResult.success(List.of(scope)));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.create(scope);

        assertThat(res).isFailed().detail().contains("already exists");
        verify(store, never()).save(any());
    }

    @Test
    void update_should_save_when_scope_exists() {
        var scope = DcpScope.Builder.newInstance().id("s1").value("v").profile("p").build();
        when(store.query(any())).thenReturn(StoreResult.success(List.of(scope)));
        when(store.save(scope)).thenReturn(StoreResult.success());

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.update(scope);

        assertThat(res).isSucceeded();
        verify(store).save(scope);
    }

    @Test
    void update_should_return_not_found_when_scope_does_not_exist() {
        var scope = DcpScope.Builder.newInstance().id("s1").value("v").profile("p").build();
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.update(scope);

        assertThat(res).isFailed().detail().contains("does not exist");
        verify(store, never()).save(any());
    }

    @Test
    void query_should_return_list_from_store() {
        var s1 = DcpScope.Builder.newInstance().id("s1").value("v1").profile("p").build();
        var expected = List.of(s1);
        when(store.query(any())).thenReturn(StoreResult.success(expected));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.query(QuerySpec.max());

        assertThat(res).isSucceeded().isEqualTo(expected);
        verify(store).query(QuerySpec.max());
    }

    @Test
    void remove_should_delegate_to_store_and_return_success() {
        when(store.delete("id")).thenReturn(StoreResult.success());

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.remove("id");

        assertThat(res).isSucceeded();

        verify(store).delete("id");
    }

    @Test
    void getDefaultScopes_should_return_list_from_store() {
        var s1 = DcpScope.Builder.newInstance().id("d1").value("v1").profile(DcpScope.WILDCARD).build();
        var s2 = DcpScope.Builder.newInstance().id("d2").value("v2").profile("p").build();
        var expected = List.of(s1, s2);

        when(store.query(any())).thenReturn(StoreResult.success(expected));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.getDefaultScopes();

        assertThat(res).isSucceeded().isEqualTo(expected);
        verify(store).query(any());
    }

    @Test
    void getScopeMapping_should_return_policy_scopes_from_store() {
        var p1 = DcpScope.Builder.newInstance().id("p1").value("v1").type(DcpScope.Type.POLICY).profile("p").prefixMapping("pm").build();
        var expected = List.of(p1);
        when(store.query(any())).thenReturn(StoreResult.success(expected));

        var impl = new DcpScopeRegistryImpl(transactionContext, store);
        var res = impl.getScopeMapping();

        assertThat(res).isSucceeded().isEqualTo(expected);
        verify(store).query(any());
    }
}
