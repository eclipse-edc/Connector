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

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.edc.sql.ResultSetMapper;
import org.eclipse.edc.sql.testfixtures.PostgresqlLocalInstance;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import javax.sql.DataSource;

import static java.time.ZoneOffset.UTC;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresLeaseContextTest extends LeaseContextTest {
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final TestEntityLeaseStatements dialect = new TestEntityLeaseStatements();
    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = spy(PostgresqlLocalInstance.getTestConnection());
    private SqlLeaseContextBuilder builder;
    private SqlLeaseContext leaseContext;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createTestDatabase();
    }

    @BeforeEach
    void setup() throws SQLException, IOException {
        when(dataSource.getConnection()).thenReturn(connection);
        doNothing().when(connection).close();

        var schema = Files.readString(Paths.get("./src/test/resources/schema.sql"));
        transactionContext.execute(() -> executeQuery(connection, schema));

        builder = SqlLeaseContextBuilder.with(transactionContext, LEASE_HOLDER, dialect, Clock.fixed(now, UTC));
        leaseContext = createLeaseContext(LEASE_HOLDER);
    }

    @AfterEach
    void teardown() throws SQLException {
        transactionContext.execute(() -> {
            executeQuery(connection, "DROP TABLE " + dialect.getLeaseTableName() + " CASCADE");
            executeQuery(connection, "DROP TABLE " + dialect.getEntityTableName() + " CASCADE");
        });
        doCallRealMethod().when(connection).close();
        connection.close();
    }

    @Override
    protected SqlLeaseContext createLeaseContext(String holder) {
        return builder.by(holder).withConnection(connection);
    }

    @Override
    protected SqlLeaseContext getLeaseContext() {
        return leaseContext;
    }

    @Override
    protected boolean isLeased(String entityId) {
        return transactionContext.execute(() -> {
            var entity = getTestEntity(entityId);
            return entity.getLeaseId() != null;
        });
    }

    @Override
    protected void insertTestEntity(String id) {
        transactionContext.execute(() -> {
            var stmt = "INSERT INTO " + dialect.getEntityTableName() + " (id) VALUES (?);";
            executeQuery(connection, stmt, id);
        });
    }

    @Override
    protected TestEntity getTestEntity(String id) {
        return transactionContext.execute(() -> {
            var stmt = "SELECT * FROM " + dialect.getEntityTableName() + " WHERE id=?";

            try (var stream = executeQuery(connection, false, map(), stmt, id)) {
                return stream.findFirst().orElse(null);
            }
        });
    }

    private ResultSetMapper<TestEntity> map() {
        return (rs) -> new TestEntity(rs.getString("id"), rs.getString("lease_id"));
    }

}
