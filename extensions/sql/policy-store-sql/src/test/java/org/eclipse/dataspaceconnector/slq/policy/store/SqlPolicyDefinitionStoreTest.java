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
 *
 */

package org.eclipse.dataspaceconnector.slq.policy.store;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.policy.store.PostgressStatements;
import org.eclipse.dataspaceconnector.sql.policy.store.SqlPolicyDefinitionStore;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ComponentTest
class SqlPolicyDefinitionStoreTest {

    private static final String DATASOURCE_NAME = "policy";
    private SqlPolicyDefinitionStore sqlPolicyStore;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        var transactionContext = new NoopTransactionContext();
        DataSourceRegistry dataSourceRegistry = mock(DataSourceRegistry.class);

        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        // do not actually close
        connection = spy(jdbcDataSource.getConnection());
        doNothing().when(connection).close();

        var datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);
        sqlPolicyStore = new SqlPolicyDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new TypeManager(), new PostgressStatements());

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        transactionContext.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));
    }

    @AfterEach
    void tearDown() throws Exception {
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    @DisplayName("Save a single policy that not exists ")
    void save_notExisting() {
        var policy = getDummyPolicy(getRandomId());

        sqlPolicyStore.save(policy);

        var policyFromDb = sqlPolicyStore.findById(policy.getUid());
        assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Save (update) a single policy that already exists")
    void save_alreadyExists() {
        var id = getRandomId();
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

        sqlPolicyStore.save(policy1);
        sqlPolicyStore.save(policy2);
        var policyFromDb = sqlPolicyStore.findAll(spec).collect(Collectors.toList());

        assertThat(1).isEqualTo(policyFromDb.size());
        assertThat("Target2").isEqualTo(policyFromDb.get(0).getPolicy().getTarget());
    }

    @Test
    @DisplayName("Find policy by ID that exists")
    void findById_whenPresent() {
        var policy = getDummyPolicy(getRandomId());
        sqlPolicyStore.save(policy);

        var policyFromDb = sqlPolicyStore.findById(policy.getUid());

        assertThat(policy).usingRecursiveComparison().isEqualTo(policyFromDb);
    }

    @Test
    @DisplayName("Find policy by ID when not exists")
    void findById_whenNonexistent() {
        assertThat(sqlPolicyStore.findById("nonexistent")).isNull();
    }

    @Test
    @DisplayName("Find all policies with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = getDummyPolicies(50);
        definitionsExpected.forEach(sqlPolicyStore::save);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var policiesFromDb = sqlPolicyStore.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).hasSize(limit);
    }

    @Test
    @DisplayName("Find policies when page size larger than DB collection")
    void findAll_pageSizeLargerThanDbCollection() {
        var pageSize = 15;

        var definitionsExpected = getDummyPolicies(10);
        definitionsExpected.forEach(sqlPolicyStore::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(pageSize)
                .build();

        var policiesFromDb = sqlPolicyStore.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).isEmpty();
    }

    @Test
    @DisplayName("Find policies when page size oversteps DB collection size")
    void findAll_pageSizeLarger() {
        var limit = 5;

        var definitionsExpected = getDummyPolicies(10);
        definitionsExpected.forEach(sqlPolicyStore::save);

        var spec = QuerySpec.Builder.newInstance()
                .offset(7)
                .limit(limit)
                .build();

        var policiesFromDb = sqlPolicyStore.findAll(spec).collect(Collectors.toList());

        assertThat(policiesFromDb).size().isLessThanOrEqualTo(limit);
    }

    @Test
    @DisplayName("Delete existing policy")
    void deleteById_whenExists() {
        var policy = getDummyPolicy(getRandomId());

        sqlPolicyStore.save(policy);

        assertThat(sqlPolicyStore.deleteById(policy.getUid()).getUid()).isEqualTo(policy.getUid());
        assertThat(sqlPolicyStore.findById(policy.getUid())).isNull();
    }

    @Test
    @DisplayName("Delete a non existing policy")
    void deleteById_whenNonexistent() {
        assertThat(sqlPolicyStore.deleteById("nonexistent")).isNull();
    }

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }

    private PolicyDefinition getDummyPolicy(String id) {
        var permission = Permission.Builder.newInstance()
                .uid(id)
                .build();

        var prohibition = Prohibition.Builder.newInstance()
                .uid(id)
                .build();

        var duty = Duty.Builder.newInstance()
                .uid(id)
                .build();

        var p = Policy.Builder.newInstance()
                .permission(permission)
                .prohibition(prohibition)
                .duties(List.of(duty))
                .inheritsFrom("sampleInheritsFrom")
                .assigner("sampleAssigner")
                .assignee("sampleAssignee")
                .target("sampleTarget")
                .type(PolicyType.SET)
                .build();
        return PolicyDefinition.Builder.newInstance().uid(id).policy(p).build();
    }

    private List<PolicyDefinition> getDummyPolicies(int count) {
        return IntStream.range(0, count).mapToObj(i -> getDummyPolicy(getRandomId())).collect(Collectors.toList());
    }

}
