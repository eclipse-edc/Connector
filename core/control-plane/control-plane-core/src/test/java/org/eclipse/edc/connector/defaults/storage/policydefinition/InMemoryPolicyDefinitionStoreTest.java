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
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Prohibition;
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
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createDutyBuilder;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPermissionBuilder;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createPolicy;
import static org.eclipse.edc.connector.policy.spi.testfixtures.TestFunctions.createProhibitionBuilder;
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

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.create(createPolicyDef()));
    }

    @Test
    void deleteById_exceptionThrown() {
        doThrow(new RuntimeException()).when(manager).writeLock(any());

        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.deleteById("any-policy-id"));
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createPolicy("test-id")).forEach(d -> getPolicyDefinitionStore().create(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(query)).isInstanceOfAny(EdcPersistenceException.class);
    }

    @Test
    void update_whenPolicyNotExists() {
        var policy = createPolicy("test-id");
        var result = getPolicyDefinitionStore().update("test-id", policy);
        assertThat(result).isNull();
    }

    @Test
    void update_whenPolicyExists_updatingPolicyFields() {
        var policy = createPolicy("test-id");
        getPolicyDefinitionStore().create(policy);

        Action action = Action.Builder.newInstance().type("UPDATED_USE").build();
        var updatedPermission = Permission.Builder.newInstance().action(action).build();
        var updatedDuty = Duty.Builder.newInstance().action(action).build();
        var updatedProhibition = Prohibition.Builder.newInstance().action(action).build();

        policy.getPolicy().getPermissions().add(updatedPermission);
        policy.getPolicy().getProhibitions().add(updatedProhibition);
        policy.getPolicy().getObligations().add(updatedDuty);
        policy.getPolicy().getExtensibleProperties().put("updatedKey", "updatedValue");

        var result = getPolicyDefinitionStore().update("test-id", policy);
        assertThat(result.getPolicy().getExtensibleProperties()).containsEntry("updatedKey", "updatedValue");
        assertThat(result.getPolicy().getPermissions().get(1)).isEqualTo(updatedPermission);
        assertThat(result.getPolicy().getProhibitions().get(1)).isEqualTo(updatedProhibition);
        assertThat(result.getPolicy().getObligations().get(1)).isEqualTo(updatedDuty);
    }

    @Test
    void update_whenPolicyExists_removingPolicyFields() {
        var policy = createPolicy("test-id");
        getPolicyDefinitionStore().create(policy);

        var updatedPolicy = createPolicyDef("test-id", "updatedTarget");
        var result = getPolicyDefinitionStore().update("test-id", updatedPolicy);

        assertThat(result.getPolicy().getTarget()).isEqualTo("updatedTarget");
        assertThat(result.getPolicy().getPermissions().size()).isEqualTo(0);
        assertThat(result.getPolicy().getProhibitions().size()).isEqualTo(0);
        assertThat(result.getPolicy().getObligations().size()).isEqualTo(0);
    }

    @Test
    void update_whenPolicyExists_replacingAllFields() {
        var policy = createPolicy("test-id");
        getPolicyDefinitionStore().create(policy);

        var updatedPermission = createPermissionBuilder("updated-id").build();
        var updatedProhibition = createProhibitionBuilder("updated-id").build();
        var updatedDuty = createDutyBuilder("updated-id").build();
        Policy updatedPolicy = Policy.Builder.newInstance()
                .target("updatedTarget")
                .permission(updatedPermission)
                .prohibition(updatedProhibition)
                .duty(updatedDuty)
                .extensibleProperty("updatedKey", "updatedValue")
                .build();

        var result = getPolicyDefinitionStore().update("test-id", PolicyDefinition.Builder.newInstance().policy(updatedPolicy).build());

        assertThat(result.getPolicy().getExtensibleProperties()).containsEntry("updatedKey", "updatedValue");
        assertThat(result.getPolicy().getPermissions().get(0)).isEqualTo(updatedPermission);
        assertThat(result.getPolicy().getProhibitions().get(0)).isEqualTo(updatedProhibition);
        assertThat(result.getPolicy().getObligations().get(0)).isEqualTo(updatedDuty);
    }

    @Test
    void update_throwsException() {
        var policy = createPolicy("test-id");
        getPolicyDefinitionStore().create(policy);

        doThrow(new RuntimeException()).when(manager).writeLock(any());
        assertThatExceptionOfType(EdcPersistenceException.class).isThrownBy(() -> store.update("test-id", createPolicyDef()));
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
