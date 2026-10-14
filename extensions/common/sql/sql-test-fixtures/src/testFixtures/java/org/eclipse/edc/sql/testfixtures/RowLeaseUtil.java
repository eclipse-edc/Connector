/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.sql.testfixtures;

import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.lease.StatefulEntityStatements;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper class to acquire and verify leases that are stored on the entity row (see {@link StatefulEntityStatements}).
 */
public class RowLeaseUtil {

    private final TransactionContext transactionContext;
    private final Supplier<Connection> connectionSupplier;
    private final StatefulEntityStatements statements;
    private final String table;
    private final Clock clock;
    private final SqlQueryExecutor queryExecutor = new SqlQueryExecutor();

    public RowLeaseUtil(TransactionContext transactionContext, Supplier<Connection> connectionSupplier, StatefulEntityStatements statements, String table, Clock clock) {
        this.transactionContext = transactionContext;
        this.connectionSupplier = connectionSupplier;
        this.statements = statements;
        this.table = table;
        this.clock = clock;
    }

    /**
     * Leases the entity, overriding any existing lease.
     */
    public void leaseEntity(String id, String leaseHolder, Duration leaseDuration) {
        var sql = "UPDATE %s SET %s = ?, %s = ?, %s = ? WHERE %s = ?".formatted(table, statements.getLeasedByColumn(),
                statements.getLeasedAtColumn(), statements.getLeaseDurationColumn(), statements.getIdColumn());
        transactionContext.execute(() -> {
            try (var connection = connectionSupplier.get()) {
                queryExecutor.execute(connection, sql, leaseHolder, clock.millis(), leaseDuration.toMillis(), id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Returns true if the entity is leased by the given holder, regardless of whether the lease is expired.
     */
    public boolean isLeased(String id, String leaseHolder) {
        return leaseHolder.equals(getLeasedBy(id));
    }

    /**
     * Returns the holder of the lease of the entity, or null if the entity is not leased.
     */
    public @Nullable String getLeasedBy(String id) {
        var sql = "SELECT %s FROM %s WHERE %s = ?".formatted(statements.getLeasedByColumn(), table, statements.getIdColumn());
        return transactionContext.execute(() -> {
            try (var connection = connectionSupplier.get()) {
                return queryExecutor.single(connection, false, rs -> Optional.ofNullable(rs.getString(1)), sql, id).orElse(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
