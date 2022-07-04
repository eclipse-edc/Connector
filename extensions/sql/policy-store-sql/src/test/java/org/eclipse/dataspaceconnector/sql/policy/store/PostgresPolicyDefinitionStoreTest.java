/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.policy.store;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.policy.store.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createAction;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createDutyBuilder;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createPermissionBuilder;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createPolicy;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createPolicyBuilder;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createProhibitionBuilder;
import static org.eclipse.dataspaceconnector.sql.policy.TestFunctions.createQuery;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * This test aims to verify those parts of the policy definition store, that are specific to Postgres, e.g. JSON query
 * operators.
 */
@PostgresqlDbIntegrationTest
class PostgresPolicyDefinitionStoreTest {
    protected static final String DATASOURCE_NAME = "policydefinition";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    protected DataSourceRegistry dataSourceRegistry;
    protected Connection connection;
    protected SqlPolicyDefinitionStore store;
    private TransactionContext txManager;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }


    @BeforeEach
    void setUp() throws SQLException, IOException {
        txManager = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);


        var ds = new PGSimpleDataSource();
        ds.setServerNames(new String[]{ "localhost" });
        ds.setPortNumbers(new int[]{ 5432 });
        ds.setUser(POSTGRES_USER);
        ds.setPassword(POSTGRES_PASSWORD);
        ds.setDatabaseName(POSTGRES_DATABASE);

        // do not actually close
        connection = spy(ds.getConnection());
        doNothing().when(connection).close();

        var datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);

        var statements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        store = new SqlPolicyDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, txManager, manager, statements);

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        try {
            txManager.execute(() -> {
                executeQuery(connection, schema);
                return null;
            });
        } catch (Exception exc) {
            fail(exc);
        }
    }

    @AfterEach
    void tearDown() throws Exception {

        txManager.execute(() -> {
            var dialect = new PostgresDialectStatements();
            executeQuery(connection, "DROP TABLE " + dialect.getPolicyTable() + " CASCADE");
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    @DisplayName("Save a single policy that not exists ")
    void save_notExisting() {
        var policy = createPolicy("policyDef");

        store.save(policy);

        var policyFromDb = store.findById(policy.getUid());
        assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Save (update) a single policy that already exists")
    void save_alreadyExists() {
        var id = "policyDef";
        var policy1 = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .target("Target1")
                        .build())
                .uid(id)
                .build();
        var policy2 = PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .target("Target2")
                        .build())
                .uid(id)
                .build();
        var spec = QuerySpec.Builder.newInstance().build();

        store.save(policy1);
        store.save(policy2);
        var policyFromDb = store.findAll(spec).collect(Collectors.toList());

        assertThat(1).isEqualTo(policyFromDb.size());
        assertThat("Target2").isEqualTo(policyFromDb.get(0).getPolicy().getTarget());
    }

    @Test
    @DisplayName("Find policy by ID that exists")
    void findById_whenPresent() {
        var policy = createPolicy("policyDef");
        store.save(policy);

        var policyFromDb = store.findById(policy.getUid());

        assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Find policy by ID when not exists")
    void findById_whenNonexistent() {
        assertThat(store.findById("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Find all policies with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = createPolicies(50);
        definitionsExpected.forEach(store::save);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var policiesFromDb = store.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).hasSize(limit);
    }

    @Test
    @DisplayName("Find policies when page size larger than DB collection")
    void findAll_pageSizeLargerThanDbCollection() {
        var pageSize = 15;

        var definitionsExpected = createPolicies(10);
        definitionsExpected.forEach(store::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(pageSize)
                .build();

        var policiesFromDb = store.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).isEmpty();
    }

    @Test
    @DisplayName("Find policies when page size oversteps DB collection size")
    void findAll_pageSizeLarger() {
        var limit = 5;

        var definitionsExpected = createPolicies(10);
        definitionsExpected.forEach(store::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(7)
                .limit(limit)
                .build();

        var policiesFromDb = store.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).size().isLessThanOrEqualTo(limit);
    }

    @Test
    @DisplayName("Delete existing policy")
    void deleteById_whenExists() {
        var policy = createPolicy("policyDef");

        store.save(policy);

        assertThat(store.deleteById(policy.getUid()).getUid()).isEqualTo(policy.getUid());
        assertThat(store.findById(policy.getUid())).isNull();
    }

    @Test
    @DisplayName("Delete a non existing policy")
    void deleteById_whenNonexistent() {
        assertThat(store.deleteById("nonexistent")).isNull();
    }

    @Test
    void find_queryByProhibitions() {
        var p = createPolicyBuilder("test-policy")
                .prohibition(createProhibitionBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("prohibitions.assignee=test-assignee");
        var result = store.findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("prohibitions.action.constraint.leftExpression.value=foo");
        var result2 = store.findAll(query2);
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

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("prohibitions.fooBarProperty=someval");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByProhibitions_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .prohibition(createProhibitionBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("prohibitions.action.constraint.leftExpression.value=someval");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByPermissions() {
        var p = createPolicyBuilder("test-policy")
                .permission(createPermissionBuilder("permission1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("permissions.assignee=test-assignee");
        var result = store.findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("permissions.action.constraint.leftExpression.value=foo");
        var result2 = store.findAll(query2);
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

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("permissions.fooBarProperty=someval");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByPermissions_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .permission(createPermissionBuilder("permission1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("permissions.action.constraint.leftExpression=someval");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByDuties() {
        var p = createPolicyBuilder("test-policy")
                .duty(createDutyBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);
        store.save(createPolicy("another-policy"));

        // query by prohibition assignee
        var query = createQuery("obligations.assignee=test-assignee");
        var result = store.findAll(query);
        assertThat(result).hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef);

        //query by prohibition action constraint
        var query2 = createQuery("obligations.action.constraint.rightExpression.value=bar");
        var result2 = store.findAll(query2);
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

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("obligations.fooBarProperty=someval");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByDuties_valueNotExist() {
        var p = createPolicyBuilder("test-policy")
                .duty(createDutyBuilder("prohibition1")
                        .assignee("test-assignee")
                        .action(createAction())
                        .build())
                .build();

        var policyDef = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p).build();
        store.save(policyDef);

        // query by prohibition assignee
        var query = createQuery("obligations.action.constraint.rightExpression.value=notexist");
        var result = store.findAll(query);
        assertThat(result).isEmpty();
    }

    @Test
    void find_queryByProperty() {
        var p1 = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(p1).build();
        var p2 = createPolicyBuilder("test-policy")
                .assigner("another-test-assigner")
                .assignee("another-test-assignee")
                .build();

        var policyDef2 = PolicyDefinition.Builder.newInstance().uid("test-policy2").policy(p2).build();
        store.save(policyDef1);
        store.save(policyDef2);

        // query by prohibition assignee
        assertThat(store.findAll(createQuery("assignee=test-assignee")))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(policyDef1);
    }

    @Test
    void find_queryByProperty_notExist() {
        var policy = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(policy).build();
        store.save(policyDef1);

        // query by prohibition assignee
        assertThatThrownBy(() -> store.findAll(createQuery("notexist=foobar")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Test
    void find_queryByProperty_valueNotFound() {
        var policy = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().uid("test-policy").policy(policy).build();
        store.save(policyDef1);

        // query by prohibition assignee
        assertThat(store.findAll(createQuery("assigner=notexist")))
                .isEmpty();
    }


    private List<PolicyDefinition> createPolicies(int count) {
        return IntStream.range(0, count).mapToObj(i -> createPolicy("policyDef" + i)).collect(Collectors.toList());
    }
}