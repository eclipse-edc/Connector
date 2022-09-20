/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *       Microsoft Corporation - updates, refactoring
 *
 */

package org.eclipse.dataspaceconnector.spi.policy.store;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createAction;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createDutyBuilder;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPermissionBuilder;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPolicies;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPolicy;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPolicyBuilder;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createProhibitionBuilder;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createQuery;


public abstract class PolicyDefinitionStoreTestBase {

    public PolicyDefinitionStoreTestBase() {
        System.setProperty("policydefinitionstore.supports.collectionQuery", String.valueOf(supportCollectionQuery()));
        System.setProperty("policydefinitionstore.supports.collectionIndexQuery", String.valueOf(supportCollectionIndexQuery()));
        System.setProperty("policydefinitionstore.supports.sortorder", String.valueOf(supportSortOrder()));

    }

    @Test
    @DisplayName("Save a single policy that not exists ")
    void save_notExisting() {
        var policy = createPolicy(getRandomId());

        getPolicyDefinitionStore().save(policy);

        var policyFromDb = getPolicyDefinitionStore().findById(policy.getUid());
        Assertions.assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Save (update) a single policy that already exists")
    void save_alreadyExists() {
        var id = getRandomId();
        var policy1 = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .target("Target1")
                        .build())
                .id(id)
                .build();
        var policy2 = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .target("Target2")
                        .build())
                .id(id)
                .build();
        var spec = QuerySpec.Builder.newInstance().build();

        getPolicyDefinitionStore().save(policy1);
        getPolicyDefinitionStore().save(policy2);
        var policyFromDb = getPolicyDefinitionStore().findAll(spec).collect(Collectors.toList());

        assertThat(1).isEqualTo(policyFromDb.size());
        assertThat("Target2").isEqualTo(policyFromDb.get(0).getPolicy().getTarget());
        Assertions.assertThat(policyFromDb.get(0)).extracting(PolicyDefinition::getCreatedAt).isEqualTo(policy2.getCreatedAt());
    }

    @Test
    @DisplayName("Find policy by ID that exists")
    void findById_whenPresent() {
        var policy = createPolicy(getRandomId());
        getPolicyDefinitionStore().save(policy);

        var policyFromDb = getPolicyDefinitionStore().findById(policy.getUid());

        Assertions.assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Find policy by ID when not exists")
    void findById_whenNonexistent() {
        Assertions.assertThat(getPolicyDefinitionStore().findById("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Find all policies with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = createPolicies(50);
        definitionsExpected.forEach(getPolicyDefinitionStore()::save);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var policiesFromDb = getPolicyDefinitionStore().findAll(spec).collect(Collectors.toList());

        Assertions.assertThat(policiesFromDb).hasSize(limit);
    }

    @Test
    @DisplayName("Find policies when page size larger than DB collection")
    void findAll_pageSizeLargerThanDbCollection() {
        var pageSize = 15;

        var definitionsExpected = createPolicies(10);
        definitionsExpected.forEach(getPolicyDefinitionStore()::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(pageSize)
                .build();

        var policiesFromDb = getPolicyDefinitionStore().findAll(spec).collect(Collectors.toList());

        Assertions.assertThat(policiesFromDb).isEmpty();
    }

    @Test
    @DisplayName("Find policies when page size oversteps DB collection size")
    void findAll_pageSizeLarger() {
        var limit = 5;

        var definitionsExpected = createPolicies(10);
        definitionsExpected.forEach(getPolicyDefinitionStore()::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(7)
                .limit(limit)
                .build();

        var policiesFromDb = getPolicyDefinitionStore().findAll(spec).collect(Collectors.toList());

        Assertions.assertThat(policiesFromDb).size().isLessThanOrEqualTo(limit);
    }

    @Test
    @DisplayName("Delete existing policy")
    void deleteById_whenExists() {
        var policy = createPolicy(getRandomId());

        getPolicyDefinitionStore().save(policy);
        assertThat(getPolicyDefinitionStore().findById(policy.getUid())).usingRecursiveComparison().isEqualTo(policy);


        Assertions.assertThat(getPolicyDefinitionStore().deleteById(policy.getUid()).getUid()).isEqualTo(policy.getUid());
        Assertions.assertThat(getPolicyDefinitionStore().findById(policy.getUid())).isNull();
    }

    @Test
    @DisplayName("Delete a non existing policy")
    void deleteById_whenNonexistent() {
        Assertions.assertThat(getPolicyDefinitionStore().deleteById("nonexistent")).isNull();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByProhibitions() {
        var p = createPolicyBuilder("test-policy")
                .prohibition(createProhibitionBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.prohibitions.assignee=test-assignee");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("policy.prohibitions.action.constraint.leftExpression.value=foo");
        var result2 = getPolicyDefinitionStore().findAll(query2);
        assertThat(result2).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);
    }

    @Test
    void find_queryByProhibitions_propertyNotExist() {
        var p = createPolicyBuilder("test-policy")
                .prohibition(createProhibitionBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.prohibitions.fooBarProperty=someval");
        assertThat(getPolicyDefinitionStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByProhibitions_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .prohibition(createProhibitionBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.prohibitions.action.constraint.leftExpression.value=someval");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByPermissions() {
        var p = createPolicyBuilder("test-policy")
                .permission(createPermissionBuilder("permission1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.permissions.assignee=test-assignee");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("policy.permissions.action.constraint.leftExpression.value=foo");
        var result2 = getPolicyDefinitionStore().findAll(query2);
        assertThat(result2).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);
    }

    @Test
    void find_queryByPermissions_propertyNotExist() {
        var p = createPolicyBuilder("test-policy")
                .permission(createPermissionBuilder("permission1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.permissions.fooBarProperty=someval");
        assertThat(getPolicyDefinitionStore().findAll(query)).isEmpty();
    }

    @Test
    void find_queryByPermissions_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .permission(createPermissionBuilder("permission1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.permissions.action.constraint.leftExpression=someval");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDuties() {
        var p = createPolicyBuilder("test-policy")
                .duty(createDutyBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);
        getPolicyDefinitionStore().save(createPolicy("another-policy"));

        // query by prohibition assignee
        var query = createQuery("policy.obligations.assignee=test-assignee");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("policy.obligations.action.constraint.rightExpression.value=bar");
        var result2 = getPolicyDefinitionStore().findAll(query2);
        assertThat(result2).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);
    }

    @Test
    void find_queryByDuties_propertyNotExist() {
        var p = createPolicyBuilder("test-policy")
                .duty(createDutyBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.obligations.fooBarProperty=someval");
        assertThat(getPolicyDefinitionStore().findAll(query)).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionQuery", matches = "true", disabledReason = "This test only runs if querying collection fields is supported")
    void find_queryByDuties_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .duty(createDutyBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p).build();
        getPolicyDefinitionStore().save(policyDef);

        // query by prohibition assignee
        var query = createQuery("policy.obligations.action.constraint.rightExpression.value=notexist");
        var result = getPolicyDefinitionStore().findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByProperty() {
        var p1 = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().id("test-policy").policy(p1).build();
        var p2 = createPolicyBuilder("test-policy")
                .assigner("another-test-assigner")
                .assignee("another-test-assignee")
                .build();

        var policyDef2 = PolicyDefinition.Builder.newInstance().id("test-policy2").policy(p2).build();
        getPolicyDefinitionStore().save(policyDef1);
        getPolicyDefinitionStore().save(policyDef2);

        // query by prohibition assignee
        assertThat(getPolicyDefinitionStore().findAll(createQuery("policy.assignee=test-assignee")))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef1);
    }

    @Test
    void find_queryByProperty_valueNotFound() {
        var policy = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().id("test-policy").policy(policy).build();
        getPolicyDefinitionStore().save(policyDef1);

        // query by prohibition assignee
        assertThat(getPolicyDefinitionStore().findAll(createQuery("policy.assigner=notexist")))
                .isEmpty();
    }

    @Test
    void findAll_whenNoFiltersWithLimitAndOffset() {
        var policy1 = createPolicy(getRandomId());
        var policy2 = createPolicy(getRandomId());
        var policy3 = createPolicy(getRandomId());
        getPolicyDefinitionStore().save(policy1);
        getPolicyDefinitionStore().save(policy2);
        getPolicyDefinitionStore().save(policy3);

        var list = getPolicyDefinitionStore().findAll(QuerySpec.Builder.newInstance().limit(3).offset(1).build()).collect(Collectors.toList());
        assertThat(list).hasSize(2).usingRecursiveFieldByFieldElementComparator().isSubsetOf(policy1, policy2, policy3);
    }

    @Test
    void findAll_whenEqualFilter() {
        var policy1 = createPolicy(getRandomId());
        var policy2 = createPolicy(getRandomId());
        var policy3 = createPolicy(getRandomId());
        getPolicyDefinitionStore().save(policy1);
        getPolicyDefinitionStore().save(policy2);
        getPolicyDefinitionStore().save(policy3);

        assertThat(getPolicyDefinitionStore().findAll(QuerySpec.Builder.newInstance().filter("id=" + policy1.getUid()).build())).usingRecursiveFieldByFieldElementComparator().containsExactly(policy1);
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_whenSort() {
        var policy1 = createPolicy("C");
        var policy2 = createPolicy("A");
        var policy3 = createPolicy("B");

        getPolicyDefinitionStore().save(policy1);
        getPolicyDefinitionStore().save(policy2);
        getPolicyDefinitionStore().save(policy3);

        assertThat(getPolicyDefinitionStore().findAll(QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build())).usingRecursiveFieldByFieldElementComparator().containsExactly(policy2, policy3, policy1);
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_allFilters() {
        var policy1 = createPolicy("1C", "target1");
        var policy2 = createPolicy("1A", "target1");
        var policy3 = createPolicy("1B", "target1");
        var policyX = createPolicy("2X", "target2");
        var policyY = createPolicy("2Y", "target2");
        getPolicyDefinitionStore().save(policy1);
        getPolicyDefinitionStore().save(policy2);
        getPolicyDefinitionStore().save(policy3);
        getPolicyDefinitionStore().save(policyX);
        getPolicyDefinitionStore().save(policyY);

        QuerySpec uid = QuerySpec.Builder.newInstance()
                .filter("policy.target=target1")
                .sortField("id")
                .sortOrder(SortOrder.DESC)
                .offset(1)
                .limit(1)
                .build();
        assertThat(getPolicyDefinitionStore().findAll(uid)).usingRecursiveFieldByFieldElementComparator().containsExactly(policy3);
    }

    @Test
    void findAll_verifyFiltering_invalidFilterExpression() {
        IntStream.range(0, 10).mapToObj(i -> createPolicy(getRandomId())).forEach(d -> getPolicyDefinitionStore().save(d));

        var query = QuerySpec.Builder.newInstance().filter("something contains other").build();

        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(query)).isInstanceOfAny(IllegalArgumentException.class);
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.sortorder", matches = "true", disabledReason = "This test only runs if sorting is supported")
    void findAll_sorting_nonExistentProperty() {

        IntStream.range(0, 10).mapToObj(i -> createPolicy(getRandomId())).forEach((d) -> getPolicyDefinitionStore().save(d));


        var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

        var all = getPolicyDefinitionStore().findAll(query).collect(Collectors.toList());
        assertThat(all).isEmpty();
    }

    @Test
    @EnabledIfSystemProperty(named = "policydefinitionstore.supports.collectionIndexQuery", matches = "true", disabledReason = "This test only runs if collections with index is supported")
    void verify_readWriteFindAll() {
        // add an object
        var policy = createPolicy(getRandomId());
        getPolicyDefinitionStore().save(policy);
        assertThat(getPolicyDefinitionStore().findAll(QuerySpec.none())).usingRecursiveFieldByFieldElementComparator().containsExactly(policy);

        // modify the object
        var modifiedPolicy = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()

                        .permission(Permission.Builder.newInstance()
                                .target("test-asset-id")
                                .action(Action.Builder.newInstance()
                                        .type("USE")
                                        .build())
                                .build())
                        .build())
                .id(policy.getUid())
                .build();

        getPolicyDefinitionStore().save(modifiedPolicy);

        // re-read
        var all = getPolicyDefinitionStore().findAll(QuerySpec.Builder.newInstance().filter("policy.permissions[0].target=test-asset-id").build()).collect(Collectors.toList());
        assertThat(all).hasSize(1).usingRecursiveFieldByFieldElementComparator().containsExactly(modifiedPolicy);

    }


    protected abstract PolicyDefinitionStore getPolicyDefinitionStore();

    protected abstract boolean supportCollectionQuery();

    protected abstract boolean supportCollectionIndexQuery();

    protected abstract Boolean supportSortOrder();

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }
}
