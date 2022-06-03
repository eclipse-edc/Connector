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

package org.eclipse.dataspaceconnector.core.defaults.policystore;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class InMemoryPolicyDefinitionStoreTest {
    private PolicyDefinitionStore store;
    private LockManager manager;

    @BeforeEach
    void setUp() {
        manager = spy(new LockManager(new ReentrantReadWriteLock(true)));
        store = new InMemoryPolicyDefinitionStore(manager);
    }

    @Test
    void findById_whenPresent() {
        var def = createPolicyDef();
        var policy = def.getPolicy();
        store.save(def);

        assertThat(store.findById(def.getUid())).extracting(PolicyDefinition::getPolicy).isEqualTo(policy);
    }

    @Test
    void findById_whenNonexistent() {
        assertThat(store.findById("nonexistent")).isNull();
    }

    @Test
    void findAll_whenNoFiltersWithLimitAndOffset() {
        var policy1 = createPolicyDef();
        var policy2 = createPolicyDef();
        var policy3 = createPolicyDef();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        var list = store.findAll(QuerySpec.Builder.newInstance().limit(3).offset(1).build()).collect(Collectors.toList());
        assertThat(list).hasSize(2).isSubsetOf(policy1, policy2, policy3);
    }

    @Test
    void findAll_whenEqualFilter() {
        var policy1 = createPolicyDef();
        var policy2 = createPolicyDef();
        var policy3 = createPolicyDef();
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().filter("uid=" + policy1.getUid()).build())).containsExactly(policy1);
    }

    @Test
    void findAll_whenSort() {
        var policy1 = createPolicyDef("C");
        var policy2 = createPolicyDef("A");
        var policy3 = createPolicyDef("B");

        store.save(policy1);
        store.save(policy2);
        store.save(policy3);

        assertThat(store.findAll(QuerySpec.Builder.newInstance().sortField("uid").sortOrder(SortOrder.ASC).build())).containsExactly(policy2, policy3, policy1);
    }

    @Test
    void findAll_allFilters() {
        var policy1 = createPolicyDef("1C", "target1");
        var policy2 = createPolicyDef("1A", "target1");
        var policy3 = createPolicyDef("1B", "target1");
        var policyX = createPolicyDef("2X", "target2");
        var policyY = createPolicyDef("2Y", "target2");
        store.save(policy1);
        store.save(policy2);
        store.save(policy3);
        store.save(policyX);
        store.save(policyY);

        QuerySpec uid = QuerySpec.Builder.newInstance()
                .filter("policy.target=target1")
                .sortField("uid")
                .sortOrder(SortOrder.DESC)
                .offset(1)
                .limit(1)
                .build();
        assertThat(store.findAll(uid)).containsExactly(policy3);
    }

    @Test
    void deleteById_whenPresent() {
        var policy = createPolicyDef();
        store.save(policy);
        assertThat(store.findById(policy.getUid())).isEqualTo(policy);

        assertThat(store.deleteById(policy.getUid())).isEqualTo(policy);
        assertThat(store.findById(policy.getUid())).isNull();
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

    private PolicyDefinition createPolicyDef() {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
    }

    private PolicyDefinition createPolicyDef(String id) {
        return PolicyDefinition.Builder.newInstance()
                .uid(id)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private PolicyDefinition createPolicyDef(String id, String target) {
        return PolicyDefinition.Builder.newInstance().uid(id).policy(Policy.Builder.newInstance().target(target).build()).build();
    }
}