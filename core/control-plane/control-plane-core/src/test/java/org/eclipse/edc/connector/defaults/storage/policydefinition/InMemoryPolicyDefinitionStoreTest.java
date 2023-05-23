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

package org.eclipse.edc.connector.defaults.storage.policydefinition;

import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.util.concurrency.LockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicy;
import static org.eclipse.edc.spi.query.Criterion.criterion;
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
        var result = store.delete("nonexistent");
        assertThat(result).isNotNull();
        assertThat(result.succeeded()).isFalse();
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

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.create(createPolicyDef()));
    }

    @Test
    void deleteById_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).writeLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.delete("any-policy-id"));
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createPolicy("test-id")).forEach(d -> getPolicyDefinitionStore().create(d));

        var query = QuerySpec.Builder.newInstance().filter(criterion("something", "contains", "other")).build();

        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(query)).isInstanceOfAny(EdcPersistenceException.class);
    }

    @Test
    void update_throwsException() {
        var policy = createPolicy("test-id");
        getPolicyDefinitionStore().create(policy);

        doThrow(new RuntimeException()).when(manager).writeLock(any());
        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.update(createPolicyDef()));
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


}
