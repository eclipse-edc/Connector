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

package org.eclipse.dataspaceconnector.sql.contractdefinition.store;


import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.contract.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.dataspaceconnector.contract.offer.store.TestFunctions;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.BaseSqlDialectStatements;
import org.eclipse.dataspaceconnector.sql.contractdefinition.store.schema.postgres.PostgresDialectStatements;
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
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {
    protected static final String DATASOURCE_NAME = "contractdefinition";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private TransactionContext transactionContext;
    private Connection connection;
    private DataSourceRegistry dataSourceRegistry;
    private BaseSqlDialectStatements statements;
    private SqlContractDefinitionStore sqlContractDefinitionStore;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }

    @BeforeEach
    void setUp() throws SQLException, IOException {
        transactionContext = new NoopTransactionContext();
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

        statements = new PostgresDialectStatements();
        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        sqlContractDefinitionStore = new SqlContractDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, statements, manager);
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        try {
            transactionContext.execute(() -> {
                executeQuery(connection, schema);
                return null;
            });
        } catch (Exception exc) {
            fail(exc);
        }
    }

    @AfterEach
    void tearDown() throws Exception {

        transactionContext.execute(() -> {
            var dialect = new PostgresDialectStatements();
            executeQuery(connection, "DROP TABLE " + dialect.getContractDefinitionTable() + " CASCADE");
        });
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
    @DisplayName("Verify empty result when query contains invalid keys")
    void findAll_queryByInvalidKey() {

        var definitionsExpected = TestFunctions.createContractDefinitions(20);
        getContractDefinitionStore().save(definitionsExpected);

        var spec = QuerySpec.Builder.newInstance()
                .filter(List.of(new Criterion("notexist", "=", "somevalue")))
                .build();

        assertThatThrownBy(() -> getContractDefinitionStore().findAll(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");

    }

    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return sqlContractDefinitionStore;
    }


    @Override
    protected Boolean supportCollectionQuery() {
        return true;
    }

    @Override
    protected Boolean supportSortOrder() {
        return false;
    }
}
