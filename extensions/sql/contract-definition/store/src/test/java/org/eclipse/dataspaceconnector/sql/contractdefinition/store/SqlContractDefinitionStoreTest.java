/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Tests
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.persistence.EdcPersistenceException;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionFactoryDataSource;
import org.eclipse.dataspaceconnector.sql.datasource.ConnectionPoolDataSource;
import org.eclipse.dataspaceconnector.sql.pool.ConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPool;
import org.eclipse.dataspaceconnector.sql.pool.commons.CommonsConnectionPoolConfig;
import org.eclipse.dataspaceconnector.transaction.local.DataSourceResource;
import org.eclipse.dataspaceconnector.transaction.local.LocalDataSourceRegistry;
import org.eclipse.dataspaceconnector.transaction.local.LocalTransactionContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

public class SqlContractDefinitionStoreTest {

    private static final String DATASOURCE_NAME = "contractdefinition";

    private DataSourceRegistry dataSourceRegistry;
    private SqlContractDefinitionStore sqlContractDefinitionStore;
    private ConnectionPool connectionPool;

    @BeforeEach
    void setUp() throws SQLException {
        var monitor = new ConsoleMonitor();
        var txManager = new LocalTransactionContext(monitor);
        dataSourceRegistry = new LocalDataSourceRegistry(txManager);
        var transactionContext = (TransactionContext) txManager;
        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        var connection = jdbcDataSource.getConnection();
        var dataSource = new ConnectionFactoryDataSource(() -> connection);
        connectionPool = new CommonsConnectionPool(dataSource, CommonsConnectionPoolConfig.Builder.newInstance().build());
        var poolDataSource = new ConnectionPoolDataSource(connectionPool);
        dataSourceRegistry.register(DATASOURCE_NAME, poolDataSource);
        txManager.registerResource(new DataSourceResource(poolDataSource));
        sqlContractDefinitionStore = new SqlContractDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, new ObjectMapper());

        try (var inputStream = this.getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        connectionPool.close();
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads() throws SQLException {
        var query = String.format("SELECT 1 FROM %s", SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE);
        var result = executeQuery(dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection(), query);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Save a single Contract Definition that doesn't already exist")
    void saveOne_doesntExist() {
        var definition = getContractDefinition("id", "contract", "policy");
        sqlContractDefinitionStore.save(definition);

        var definitions = sqlContractDefinitionStore.findAll();

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Save a single Contract Definition that already exists")
    void saveOne_alreadyExist() {
        var definition = getContractDefinition("id", "contract", "policy");
        sqlContractDefinitionStore.save(definition);

        assertThatThrownBy(() -> sqlContractDefinitionStore.save(definition)).isInstanceOf(EdcPersistenceException.class);
    }

    @Test
    @DisplayName("Save a single Contract Definition that is identical to an existing contract definition except for the id")
    void saveOne_sameParametersDifferentId() {
        var definition1 = getContractDefinition("id1", "contract", "policy");
        var definition2 = getContractDefinition("id2", "contract", "policy");
        sqlContractDefinitionStore.save(definition1);
        sqlContractDefinitionStore.save(definition2);

        var definitions = sqlContractDefinitionStore.findAll();

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with no preexisting Definitions")
    void saveMany_noneExist() {
        var definitionsCreated = getContractDefinitions(10);
        sqlContractDefinitionStore.save(definitionsCreated);

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(definitionsCreated.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with some preexisting Definitions")
    void saveMany_someExist() {
        var definitionsCreated = getContractDefinitions(10);
        sqlContractDefinitionStore.save(definitionsCreated.subList(0, 4));

        assertThatThrownBy(() -> sqlContractDefinitionStore.save(definitionsCreated)).isInstanceOf(EdcPersistenceException.class);

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with all preexisting Definitions")
    void saveMany_allExist() {
        var definitionsCreated = getContractDefinitions(10);
        sqlContractDefinitionStore.save(definitionsCreated);

        assertThatThrownBy(() -> sqlContractDefinitionStore.save(definitionsCreated)).isInstanceOf(EdcPersistenceException.class);

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(definitionsCreated.size());
    }

    @Test
    @DisplayName("Update a non-existing Contract Definition")
    void updateOne_doesNotExist() {
        var definition = getContractDefinition("id", "contract1", "policy1");

        assertThatThrownBy(() -> sqlContractDefinitionStore.update(definition))
                .isInstanceOf(EdcPersistenceException.class)
                .hasMessageContaining(String.format("Cannot update. Contract Definition with ID '%s' does not exist.", definition.getId()));
    }

    @Test
    @DisplayName("Update an existing Contract Definition")
    void updateOne_exists() throws SQLException {
        var definition1 = getContractDefinition("id", "contract1", "policy1");
        var definition2 = getContractDefinition("id", "contract2", "policy2");

        sqlContractDefinitionStore.save(definition1);
        sqlContractDefinitionStore.update(definition2);

        var query = String.format("SELECT * FROM %s WHERE %s=?",
                SqlContractDefinitionTables.CONTRACT_DEFINITION_TABLE,
                SqlContractDefinitionTables.CONTRACT_DEFINITION_COLUMN_ID);

        var definitions = executeQuery(dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection(),
                ((SqlContractDefinitionStore) sqlContractDefinitionStore)::mapResultSet,
                query,
                definition1.getId());

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
        assertThat(definitions.get(0).getContractPolicy().getUid()).isEqualTo(definition2.getContractPolicy().getUid());
        assertThat(definitions.get(0).getAccessPolicy().getUid()).isEqualTo(definition2.getAccessPolicy().getUid());
    }

    @Test
    @DisplayName("Find all contract definitions")
    void findAll() {
        var definitionsExpected = getContractDefinitions(10);
        sqlContractDefinitionStore.save(definitionsExpected);

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(definitionsExpected.size());
    }

    @Test
    @DisplayName("Find all contract definitions with limit and offset")
    void findAll_withSpec() {
        var limit = 20;

        var definitionsExpected = getContractDefinitions(50);
        sqlContractDefinitionStore.save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .limit(limit)
                .offset(20)
                .build();

        var definitionsRetrieved = sqlContractDefinitionStore.findAll(spec).collect(Collectors.toList());

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(limit);
    }

    private ContractDefinition getContractDefinition(String id, String contractId, String policyId) {
        var accessPolicy = Policy.Builder.newInstance()
                .id(policyId)
                .build();
        var contractPolicy = Policy.Builder.newInstance()
                .id(contractId)
                .build();
        var expression = AssetSelectorExpression.Builder.newInstance()
                .build();

        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicy(accessPolicy)
                .contractPolicy(contractPolicy)
                .selectorExpression(expression)
                .build();
    }

    private ArrayList<ContractDefinition> getContractDefinitions(int count) {
        var definitions = new ArrayList<ContractDefinition>();

        for (int i = 0; i < count; i++) {
            definitions.add(getContractDefinition("id" + i, "contract" + i, "policy" + i));
        }

        return definitions;
    }

}
