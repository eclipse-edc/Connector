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

package org.eclipse.dataspaceconnector.sql.lease;


import org.eclipse.dataspaceconnector.spi.persistence.LeaseContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.dataspaceconnector.sql.SqlQueryExecutor.executeQuery;

/**
 * SQL-based implementation of the LeaseContext.
 * Acquiring a lease is implemented by adding an entry into the "lease" table in the database
 * Breaking a lease is implemented by deleting the respective entry
 */
public class SqlLeaseContext implements LeaseContext {
    private static final long DEFAULT_LEASE_DURATION = 60_000;
    private final TransactionContext trxContext;
    private final LeaseStatements statements;
    private final String leaseHolder;
    private final Connection connection;
    private final Duration leaseDuration;


    SqlLeaseContext(TransactionContext trxContext, LeaseStatements statements, String leaseHolder, Duration leaseDuration, Connection connection) {
        this.trxContext = trxContext;
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.leaseDuration = leaseDuration;
        this.connection = connection;
    }

    @Override
    public void breakLease(String entityId) {
        trxContext.execute(() -> {

            var l = getLease(entityId);

            if (l != null) {
                if (!Objects.equals(leaseHolder, l.getLeasedBy())) {
                    throw new IllegalStateException("Current runtime does not hold the lease for Object (id [" + entityId + "]), cannot break lease!");
                }

                var stmt = statements.getDeleteLeaseTemplate();
                executeQuery(connection, stmt, l.getLeaseId());
            }
        });
    }

    @Override
    public void acquireLease(String entityId) {
        trxContext.execute(() -> {
            var now = Instant.now().toEpochMilli();

            var lease = getLease(entityId);

            if (lease != null && !lease.isExpired()) {
                throw new IllegalStateException("Entity is currently leased!");
            }

            //clean out old lease if present
            var deleteStmt = statements.getDeleteLeaseTemplate();
            executeQuery(connection, deleteStmt, entityId);

            // create new lease in DB
            var id = UUID.randomUUID().toString();
            var duration = leaseDuration != null ? leaseDuration.toMillis() : DEFAULT_LEASE_DURATION;
            var stmt = statements.getInsertLeaseTemplate();
            executeQuery(connection, stmt, id, leaseHolder, now, duration);

            //update entity with lease -> effectively lease entity
            var updStmt = statements.getUpdateLeaseTemplate();
            executeQuery(connection, updStmt, id, entityId);

        });
    }

    /**
     * Fetches a lease for a particular entity
     *
     * @param entityId The leased entity's ID (NOT the leaseID!)
     * @return The respective lease, or null of entity is not leased.
     */
    public @Nullable SqlLease getLease(String entityId) {
        var stmt = statements.getFindLeaseByEntityTemplate();
        var leases = executeQuery(connection, this::mapLease, stmt, entityId);

        return leases.stream().findFirst().orElse(null);
    }

    private SqlLease mapLease(ResultSet resultSet) throws SQLException {
        var lease = new SqlLease(resultSet.getString(statements.getLeasedByColumn()),
                resultSet.getLong(statements.getLeasedAtColumn()),
                resultSet.getLong(statements.getLeaseDurationColumn()));
        lease.setLeaseId(resultSet.getString(statements.getLeaseIdColumn()));
        return lease;
    }
}
