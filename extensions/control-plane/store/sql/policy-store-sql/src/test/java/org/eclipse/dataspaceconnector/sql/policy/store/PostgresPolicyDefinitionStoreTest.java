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
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStoreTestBase;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.policy.store.schema.postgres.PostgresDialectStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createPolicyBuilder;
import static org.eclipse.dataspaceconnector.spi.policy.TestFunctions.createQuery;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
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
class PostgresPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {
    protected static final String DATASOURCE_NAME = "policydefinition";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    protected DataSourceRegistry dataSourceRegistry;
    protected Connection connection;
    private TransactionContext txManager;
    private SqlPolicyDefinitionStore sqlPolicyStore;

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
        sqlPolicyStore = new SqlPolicyDefinitionStore(dataSourceRegistry, DATASOURCE_NAME, txManager, manager, statements);

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
    void find_queryByProperty_notExist() {
        var policy = createPolicyBuilder("test-policy")
                .assigner("test-assigner")
                .assignee("test-assignee")
                .build();

        var policyDef1 = PolicyDefinition.Builder.newInstance().id("test-policy").policy(policy).build();
        getPolicyDefinitionStore().save(policyDef1);

        // query by prohibition assignee
        assertThatThrownBy(() -> getPolicyDefinitionStore().findAll(createQuery("notexist=foobar")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Translation failed for Model");
    }

    @Override
    protected SqlPolicyDefinitionStore getPolicyDefinitionStore() {
        return sqlPolicyStore;
    }

    @Override
    protected boolean supportCollectionQuery() {
        return true;
    }

    @Override
    protected Boolean supportSortOrder() {
        return false;
    }
}
