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
 *       SAP SE - bugfix (pass correct lease id for deletion)
 *
 */

package org.eclipse.edc.sql.lease;


import org.eclipse.edc.spi.persistence.LeaseContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

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
    private final String resourceKind;
    private final Connection connection;
    private final Clock clock;
    private final Duration leaseDuration;
    private final QueryExecutor queryExecutor;


    SqlLeaseContext(TransactionContext trxContext, LeaseStatements statements, String leaseHolder, String resourceKind, Clock clock, Duration leaseDuration, Connection connection, QueryExecutor queryExecutor) {
        this.trxContext = trxContext;
        this.statements = statements;
        this.leaseHolder = leaseHolder;
        this.resourceKind = resourceKind;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.connection = connection;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public void breakLease(String entityId) {
        trxContext.execute(() -> {

            var l = getLease(entityId);

            if (l != null) {
                if (!Objects.equals(leaseHolder, l.getLeasedBy())) {
                    throw new IllegalStateException("Current runtime does not hold the lease for Object (id [%s], kind [%s]), cannot break lease!".formatted(entityId, resourceKind));
                }

                var stmt = statements.getDeleteLeaseTemplate();
                queryExecutor.execute(connection, stmt, l.getResourceId(), resourceKind);
            }
        });
    }

    @Override
    public void acquireLease(String entityId) {
        trxContext.execute(() -> {
            var now = clock.millis();
            var duration = leaseDuration != null ? leaseDuration.toMillis() : DEFAULT_LEASE_DURATION;
            var upsertStmt = statements.getUpsertLeaseTemplate();
            var result = queryExecutor.execute(connection, upsertStmt, entityId, leaseHolder, resourceKind, now, duration, now);
            if (result == 0) {
                throw new IllegalStateException("Entity %s of kind %s is currently leased!".formatted(entityId, resourceKind));
            }
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
        return queryExecutor.single(connection, false, this::mapLease, stmt, entityId, resourceKind);
    }

    private SqlLease mapLease(ResultSet resultSet) throws SQLException {
        return new SqlLease(resultSet.getString(statements.getLeasedByColumn()),
                resultSet.getString(statements.getResourceIdColumn()),
                resultSet.getString(statements.getResourceKindColumn()),
                resultSet.getLong(statements.getLeasedAtColumn()),
                resultSet.getLong(statements.getLeaseDurationColumn()));
    }
}
