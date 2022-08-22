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

package org.eclipse.dataspaceconnector.sql.lease;

import org.eclipse.dataspaceconnector.common.util.junit.annotations.PostgresqlDbIntegrationTest;
import org.eclipse.dataspaceconnector.common.util.postgres.PostgresqlLocalInstance;
import org.eclipse.dataspaceconnector.policy.model.PolicyRegistrationTypes;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.datasource.DataSourceRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.sql.ResultSetMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;

import static java.time.ZoneOffset.UTC;
import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@PostgresqlDbIntegrationTest
class PostgresLeaseContextTest extends LeaseContextTest {
    private static final String DATASOURCE_NAME = "lease-test";
    private static final String POSTGRES_USER = "postgres";
    private static final String POSTGRES_PASSWORD = "password";
    private static final String POSTGRES_DATABASE = "itest";
    private SqlLeaseContext leaseContext;
    private TransactionContext transactionContext;
    private Connection connection;
    private SqlLeaseContextBuilder builder;
    private TestEntityLeaseStatements dialect;

    @BeforeAll
    static void prepare() {
        PostgresqlLocalInstance.createDatabase(POSTGRES_DATABASE);
    }

    @BeforeEach
    void setup() throws SQLException {
        dialect = new TestEntityLeaseStatements();

        transactionContext = new NoopTransactionContext();
        DataSourceRegistry dataSourceRegistry = mock(DataSourceRegistry.class);


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

        TypeManager manager = new TypeManager();

        manager.registerTypes(PolicyRegistrationTypes.TYPES.toArray(Class<?>[]::new));
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            var schema = new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
            transactionContext.execute(() -> executeQuery(connection, schema));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

            var res = executeQuery(connection, map(), stmt, id);

            return res.stream().findFirst().orElse(null);
        });
    }

    private ResultSetMapper<TestEntity> map() {
        return (rs) -> new TestEntity(rs.getString("id"), rs.getString("lease_id"));
    }

    private static class TestEntityLeaseStatements implements LeaseStatements {

        @Override
        public String getDeleteLeaseTemplate() {
            return "DELETE FROM edc_lease WHERE lease_id=?;";
        }

        @Override
        public String getInsertLeaseTemplate() {
            return "INSERT INTO edc_lease (lease_id, leased_by, leased_at, lease_duration) VALUES (?, ?, ?, ?);";
        }

        @Override
        public String getUpdateLeaseTemplate() {
            return "UPDATE " + getEntityTableName() + " SET lease_id=? WHERE id = ?;";
        }

        @Override
        public String getFindLeaseByEntityTemplate() {
            return "SELECT * FROM edc_lease WHERE lease_id = (SELECT lease_id FROM " + getEntityTableName() + " WHERE id=?)";
        }

        public String getEntityTableName() {
            return "edc_test_entity";
        }
    }

}
