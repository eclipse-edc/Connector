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

package org.eclipse.dataspaceconnector.dataplane.selector.store.sql;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.dataplane.selector.TestDataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.dataspaceconnector.dataplane.selector.store.sql.schema.DataPlaneInstanceStatements;
import org.eclipse.dataspaceconnector.dataplane.selector.store.sql.schema.postgres.PostgresDataPlaneInstanceStatements;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.PostgresqlLocalInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


@PostgresqlDbIntegrationTest
public class PostgresDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {

    private static final String DATASOURCE_NAME = "dataplaneinstance";

    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final DataSourceRegistry dataSourceRegistry = mock(DataSourceRegistry.class);
    private final DataPlaneInstanceStatements statements = new PostgresDataPlaneInstanceStatements();
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = spy(PostgresqlLocalInstance.getTestConnection());

    SqlDataPlaneInstanceStore store;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createTestDatabase();
    }

    @BeforeEach
    void setUp() throws IOException, SQLException {
        when(dataSourceRegistry.resolve(DATASOURCE_NAME)).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        doNothing().when(connection).close();

        var typeManager = new TypeManager();
        typeManager.registerTypes(DataPlaneInstanceImpl.class);
        typeManager.registerTypes(TestDataPlaneInstance.class);


        store = new SqlDataPlaneInstanceStore(dataSourceRegistry, DATASOURCE_NAME, transactionContext, statements, typeManager.getMapper());
        var schema = Files.readString(Paths.get("./docs/schema.sql"));
        transactionContext.execute(() -> executeQuery(connection, schema));
    }

    @AfterEach
    void tearDown() throws SQLException {
        transactionContext.execute(() -> executeQuery(connection, "DROP TABLE " + statements.getDataPlaneInstanceTable() + " CASCADE"));
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Override
    protected DataPlaneInstanceStore getStore() {
        return store;
    }
}
