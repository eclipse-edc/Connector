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
 *       Microsoft Corporation - Method signature change
 *       Microsoft Corporation - refactoring
 *       Microsoft Corporation - added tests
 *       Fraunhofer Institute for Software and Systems Engineering - added tests
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.sql.SqlQueryExecutor;
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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ComponentTest
public class SqlContractDefinitionStoreTest {

    private static final String DATASOURCE_NAME = "contractdefinition";

    private DataSourceRegistry dataSourceRegistry;
    private SqlContractDefinitionStore sqlContractDefinitionStore;
    private PostgresStatements statements;
    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        var txManager = new NoopTransactionContext();
        dataSourceRegistry = mock(DataSourceRegistry.class);

        var jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:");

        // do not actually close
        connection = spy(jdbcDataSource.getConnection());
        doNothing().when(connection).close();

        DataSource datasourceMock = mock(DataSource.class);
        when(datasourceMock.getConnection()).thenReturn(connection);
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(datasourceMock);

        statements = new PostgresStatements();
        sqlContractDefinitionStore = new SqlContractDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, txManager, statements, new TypeManager());

        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        txManager.execute(() -> SqlQueryExecutor.executeQuery(connection, schema));
    }

    @AfterEach
    void tearDown() throws Exception {
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Test
    @DisplayName("Context Loads, tables exist")
    void contextLoads() throws SQLException {
        var query = String.format("SELECT 1 FROM %s", statements.getContractDefinitionTable());
        var result = executeQuery(dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection(), query);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Save a single Contract Definition that doesn't already exist")
    void saveOne_doesntExist() {
        var definition = getContractDefinition("id", "policy", "contract");
        sqlContractDefinitionStore.save(definition);

        var definitions = sqlContractDefinitionStore.findAll();

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Save a single Contract Definition that already exists")
    void saveOne_alreadyExist_shouldUpdate() {
        sqlContractDefinitionStore.save(getContractDefinition("id", "policy", "contract"));
        sqlContractDefinitionStore.save(getContractDefinition("id", "updatedAccess", "updatedContract"));

        var result = sqlContractDefinitionStore.findAll();

        assertThat(result).hasSize(1).containsExactly(getContractDefinition("id", "updatedAccess", "updatedContract"));
    }

    @Test
    @DisplayName("Save a single Contract Definition that is identical to an existing contract definition except for the id")
    void saveOne_sameParametersDifferentId() {
        var definition1 = getContractDefinition("id1", "policy", "contract");
        var definition2 = getContractDefinition("id2", "policy", "contract");
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

        assertThat(definitionsRetrieved).hasSize(10);
        assertThat(definitionsRetrieved.size()).isEqualTo(definitionsCreated.size());
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with some preexisting Definitions")
    void saveMany_someExist() {
        sqlContractDefinitionStore.save(getContractDefinitions(3));
        sqlContractDefinitionStore.save(getContractDefinitions(10));

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).hasSize(10);
    }

    @Test
    @DisplayName("Save multiple Contract Definitions with all preexisting Definitions")
    void saveMany_allExist() {
        var definitionsCreated = getContractDefinitions(10);
        sqlContractDefinitionStore.save(definitionsCreated);

        //
        sqlContractDefinitionStore.save(definitionsCreated);

        var definitionsRetrieved = sqlContractDefinitionStore.findAll();

        assertThat(definitionsRetrieved).isNotNull();
        assertThat(definitionsRetrieved.size()).isEqualTo(definitionsCreated.size());
    }

    @Test
    @DisplayName("Update a non-existing Contract Definition")
    void updateOne_doesNotExist_shouldCreate() {
        var definition = getContractDefinition("id", "policy1", "contract1");

        sqlContractDefinitionStore.update(definition);
        var existing = sqlContractDefinitionStore.findAll();
        assertThat(existing).hasSize(1).containsExactly(definition);
    }

    @Test
    @DisplayName("Update an existing Contract Definition")
    void updateOne_exists() throws SQLException {
        var definition1 = getContractDefinition("id", "policy1", "contract1");
        var definition2 = getContractDefinition("id", "policy2", "contract2");

        sqlContractDefinitionStore.save(definition1);
        sqlContractDefinitionStore.update(definition2);

        var query = String.format("SELECT * FROM %s WHERE %s=?",
                statements.getContractDefinitionTable(),
                statements.getIdColumn());

        var definitions = executeQuery(dataSourceRegistry.resolve(DATASOURCE_NAME).getConnection(),
                ((SqlContractDefinitionStore) sqlContractDefinitionStore)::mapResultSet,
                query,
                definition1.getId());

        assertThat(definitions).isNotNull();
        assertThat(definitions.size()).isEqualTo(1);
        assertThat(definitions.get(0).getContractPolicyId()).isEqualTo(definition2.getContractPolicyId());
        assertThat(definitions.get(0).getAccessPolicyId()).isEqualTo(definition2.getAccessPolicyId());
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
    void findAll_verifyQueryDefaults() {
        var all = IntStream.range(0, 100).mapToObj(i -> getContractDefinition("id" + i, "policyId" + i, "contractId" + i))
                .peek(cd -> sqlContractDefinitionStore.save(cd))
                .collect(Collectors.toList());

        assertThat(sqlContractDefinitionStore.findAll()).hasSize(50)
                .usingRecursiveFieldByFieldElementComparator()
                .isSubsetOf(all);
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

    @Test
    void findById() {
        var id = "definitionId";
        var definition = getContractDefinition(id, "policyId", "contractId");
        sqlContractDefinitionStore.save(definition);

        var result = sqlContractDefinitionStore.findById(id);

        assertThat(result).isNotNull().isEqualTo(definition);
    }

    @Test
    void findById_invalidId() {
        assertThat(sqlContractDefinitionStore.findById("invalid-id")).isNull();
    }

    @Test
    void delete() {
        var definitionExpected = getContractDefinition("test-id1", "policy1", "contract1");
        sqlContractDefinitionStore.save(definitionExpected);
        assertThat(sqlContractDefinitionStore.findAll()).hasSize(1);

        var deleted = sqlContractDefinitionStore.deleteById("test-id1");
        assertThat(deleted).isNotNull().usingRecursiveComparison().isEqualTo(definitionExpected);
        assertThat(sqlContractDefinitionStore.findAll()).isEmpty();
    }

    @Test
    void delete_whenNotExist() {
        var deleted = sqlContractDefinitionStore.deleteById("test-id1");
        assertThat(deleted).isNull();
    }

    @Test
    void isReferenced_notReferenced() {
        var definitionsExpected = getContractDefinition("def1", "apol1", "cpol1");
        sqlContractDefinitionStore.save(definitionsExpected);

        assertThat(sqlContractDefinitionStore.isReferenced("testpol1")).isEmpty();
    }

    @Test
    void isReferenced_asAccessPolicy() {
        var definitionExpected = getContractDefinition("def1", "apol1", "cpol1");
        sqlContractDefinitionStore.save(definitionExpected);

        assertThat(sqlContractDefinitionStore.isReferenced("apol1")).usingRecursiveFieldByFieldElementComparator().containsOnly(definitionExpected);
    }

    @Test
    void isReferenced_asContractPolicy() {
        var definitionExpected = getContractDefinition("def1", "apol1", "cpol1");
        sqlContractDefinitionStore.save(definitionExpected);

        assertThat(sqlContractDefinitionStore.isReferenced("cpol1")).usingRecursiveFieldByFieldElementComparator().containsOnly(definitionExpected);
    }

    @Test
    void isReferenced_byMultipleDefinitions() {
        var def1 = getContractDefinition("def1", "apol1", "cpol1");
        var def2 = getContractDefinition("def2", "apol1", "cpol2");
        var def3 = getContractDefinition("def3", "apol1", "cpol3");
        var def4 = getContractDefinition("def4", "apol2", "cpol4");
        var def5 = getContractDefinition("def5", "apol2", "cpol1");

        sqlContractDefinitionStore.save(List.of(def1, def2, def3, def4, def5));

        assertThat(sqlContractDefinitionStore.isReferenced("apol1")).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(def1, def2, def3);
        assertThat(sqlContractDefinitionStore.isReferenced("cpol1")).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(def1, def5);

    }

    private ContractDefinition getContractDefinition(String id, String accessPolicyId, String contractPolicyId) {
        return ContractDefinition.Builder.newInstance()
                .id(id)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().build())
                .build();
    }

    private Collection<ContractDefinition> getContractDefinitions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> getContractDefinition("id" + i, "policy" + i, "contract" + i))
                .collect(Collectors.toList());
    }

}
