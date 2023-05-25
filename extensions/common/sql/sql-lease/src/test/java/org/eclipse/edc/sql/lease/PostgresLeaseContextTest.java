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
 *       SAP SE - bugfix (pass correct lease id for deletion)
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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.sql.SqlQueryExecutor.executeQuery;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    void acquireLease_whenExpiredLeasePresent_shouldDeleteOldLeaseAndAcquireNewLease() throws SQLException {
        var preparedStatementReference = new AtomicReference<PreparedStatement>();
        when(connection.prepareStatement(dialect.getDeleteLeaseTemplate(), PreparedStatement.RETURN_GENERATED_KEYS)).thenAnswer((mocks) -> {
            PreparedStatement preparedStatement = (PreparedStatement) mocks.callRealMethod();
            PreparedStatement spy = spy(preparedStatement);
            preparedStatementReference.set(spy);
            return spy;
        });

        var entityId = "test-entity";
        insertTestEntity(entityId);
        var leaseContext = createLeaseContext("someone-else");

        // no lease present, acquire one
        leaseContext.acquireLease(entityId);
        var lease = leaseContext.getLease(entityId);
        assertThat(lease).isNotNull();
        var leaseId = lease.getLeaseId();

        // should acquire lease by deleting old one after lease expiry
        var twoMinutesAheadClock = Clock.offset(Clock.fixed(now, UTC), Duration.of(2, ChronoUnit.MINUTES));
        var twoMinutesAheadBuilder = SqlLeaseContextBuilder.with(transactionContext, LEASE_HOLDER, dialect, twoMinutesAheadClock);
        var twoMinutesAheadContext = twoMinutesAheadBuilder.by("someone-else").withConnection(connection);
        twoMinutesAheadContext.acquireLease(entityId);

        var newLease = twoMinutesAheadContext.getLease(entityId);
        assertThat(newLease).isNotNull();
        assertThat(newLease.getLeaseId()).isNotEqualTo(leaseId);
        verify(connection, times(2)).prepareStatement(dialect.getDeleteLeaseTemplate(), PreparedStatement.RETURN_GENERATED_KEYS);
        verify(preparedStatementReference.get(), times(1)).setString(1, leaseId);
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
