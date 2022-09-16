/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.core.controlplane.defaults.policystore;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStoreTestBase;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPolicy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class InMemoryPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {
    private PolicyDefinitionStore store;
    private LockManager manager;

    @BeforeEach
    void setUp() {
        manager = spy(new LockManager(new ReentrantReadWriteLock(true)));
        store = new InMemoryPolicyDefinitionStore(manager);
    }


    @Test
    void deleteById_whenNonexistent() {
        assertThat(store.deleteById("nonexistent")).isNull();
    }

    @Test
    void findById_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).readLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.findById("any-policy-id"));
    }

    @Test
    void findAll_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).readLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.findAll(QuerySpec.Builder.newInstance().build()));
    }

    @Test
    void save_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).writeLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.save(createPolicyDef()));
    }

    @Test
    void deleteById_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).writeLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.deleteById("any-policy-id"));
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createPolicy("test-id")).forEach(d -> getPolicyDefinitionStore().save(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(query)).isInstanceOfAny(EdcPersistenceException.class);
    }


    @Override
    protected PolicyDefinitionStore getPolicyDefinitionStore() {
        return store;
    }

    @Override
    protected boolean supportCollectionQuery() {
        return false;
    }

    @Override
    protected boolean supportCollectionIndexQuery() {
        return true;
    }

    @Override
    protected Boolean supportSortOrder() {
        return true;
    }

    private PolicyDefinition createPolicyDef() {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
    }

    private PolicyDefinition createPolicyDef(String id) {
        return PolicyDefinition.Builder.newInstance()
                .id(id)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private PolicyDefinition createPolicyDef(String id, String target) {
        return PolicyDefinition.Builder.newInstance().id(id).policy(Policy.Builder.newInstance().target(target).build()).build();
    }
}
