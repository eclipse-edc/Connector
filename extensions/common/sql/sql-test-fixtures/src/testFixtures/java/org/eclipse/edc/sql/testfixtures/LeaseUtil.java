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

package org.eclipse.edc.sql.testfixtures;

import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.SqlQueryExecutor;
import org.eclipse.edc.sql.lease.SqlLeaseContextBuilderImpl;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Helper class to easily acquire and verify leases.
 * Wraps the {@link SqlLeaseContextBuilderImpl} for even easier handling in tests.
 */
public class LeaseUtil {

    private final SqlLeaseContextBuilderImpl leaseContextBuilder;
    private final Supplier<Connection> connectionSupplier;

    public LeaseUtil(TransactionContext context, Supplier<Connection> connectionSupplier, String resourceKind, LeaseStatements statements, Clock clock) {
        this.connectionSupplier = connectionSupplier;
        leaseContextBuilder = SqlLeaseContextBuilderImpl.with(context, "test", resourceKind, statements, clock, new SqlQueryExecutor());
    }

    public void leaseEntity(String tpId, String leaseHolder, Duration leaseDuration) {
        try (var conn = connectionSupplier.get()) {
            leaseContextBuilder.by(leaseHolder).forTime(leaseDuration).withConnection(conn).acquireLease(tpId)
                    .orElseThrow((l) -> new EdcPersistenceException(l.getFailureDetail()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isLeased(String id, String connectorName) {
        try (var conn = connectionSupplier.get()) {
            var lease = leaseContextBuilder.withConnection(conn).getLease(id);
            return lease != null && lease.getLeasedBy().equals(connectorName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
