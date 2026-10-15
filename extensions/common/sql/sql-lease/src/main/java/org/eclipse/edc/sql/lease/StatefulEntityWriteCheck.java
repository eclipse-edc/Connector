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

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.sql.QueryExecutor;

import java.sql.Connection;
import java.time.Clock;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Verifies whether a stateful entity, whose lease and version are stored on its row, can be written by a lease holder.
 */
public class StatefulEntityWriteCheck {

    private final QueryExecutor queryExecutor;
    private final StatefulEntityStatements statements;
    private final String table;
    private final String leaseHolder;
    private final Clock clock;

    public StatefulEntityWriteCheck(QueryExecutor queryExecutor, StatefulEntityStatements statements, String table, String leaseHolder, Clock clock) {
        this.queryExecutor = queryExecutor;
        this.statements = statements;
        this.table = table;
        this.leaseHolder = leaseHolder;
        this.clock = clock;
    }

    /**
     * Checks whether the entity can be written with the given version.
     *
     * @param connection the connection.
     * @param id the entity id.
     * @param version the version of the entity being written.
     * @return success if the entity does not exist yet or if it can be updated, {@code ALREADY_LEASED} if another holder
     *         holds an unexpired lease, {@code CONFLICT} if the stored version differs.
     */
    public StoreResult<Void> check(Connection connection, String id, long version) {
        return check(connection, id, version, statements.getFindVersionAndLeaseTemplate(table));
    }

    /**
     * Checks whether the entity can be written with the given version, like {@link #check(Connection, String, long)},
     * and locks its row until the end of the transaction. To be used when other rows are written before the entity, so
     * that the entity cannot be modified concurrently between the check and its write.
     *
     * @param connection the connection.
     * @param id the entity id.
     * @param version the version of the entity being written.
     * @return success if the entity does not exist yet or if it can be updated, {@code ALREADY_LEASED} if another holder
     *         holds an unexpired lease, {@code CONFLICT} if the stored version differs.
     */
    public StoreResult<Void> checkAndLock(Connection connection, String id, long version) {
        return check(connection, id, version, statements.getFindVersionAndLeaseForUpdateTemplate(table));
    }

    private StoreResult<Void> check(Connection connection, String id, long version, String template) {
        var row = queryExecutor.single(connection, false, rs -> new Row(
                        rs.getLong(statements.getVersionColumn()),
                        rs.getString(statements.getLeasedByColumn()),
                        rs.getLong(statements.getLeasedAtColumn()),
                        rs.getLong(statements.getLeaseDurationColumn())),
                template, id);

        if (row == null) {
            return StoreResult.success();
        }
        if (row.isLeasedByOther(leaseHolder, clock.millis())) {
            return StoreResult.alreadyLeased(format("Entity %s is leased by another holder, cannot update it!", id));
        }
        if (row.version() != version) {
            return StoreResult.conflict(format("Entity %s has been modified: stored version is %d, saved version is %d", id, row.version(), version));
        }
        return StoreResult.success();
    }

    /**
     * Explains why a write of the entity with the given version did not update any row.
     *
     * @param connection the connection.
     * @param id the entity id.
     * @param version the version of the entity being written.
     * @return {@code ALREADY_LEASED} if another holder holds an unexpired lease, {@code CONFLICT} otherwise.
     */
    public StoreResult<Void> explainNotWritten(Connection connection, String id, long version) {
        var result = check(connection, id, version);
        if (result.failed()) {
            return result;
        }
        // the row changed between the write and the check
        return StoreResult.conflict(format("Entity %s has been modified concurrently", id));
    }

    private record Row(long version, String leasedBy, long leasedAt, long leaseDuration) {
        boolean isLeasedByOther(String holder, long now) {
            return leasedBy != null && !Objects.equals(leasedBy, holder) && leasedAt + leaseDuration >= now;
        }
    }
}
