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

import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Helper class to easily acquire and verify leases.
 * Wraps the {@link SqlLeaseContextBuilder} for even easier handling in tests.
 */
public class LeaseUtil {

    private final SqlLeaseContextBuilder leaseContextBuilder;
    private final Supplier<Connection> connectionSupplier;

    public LeaseUtil(TransactionContext context, Supplier<Connection> connectionSupplier, LeaseStatements statements) {
        this.connectionSupplier = connectionSupplier;
        leaseContextBuilder = SqlLeaseContextBuilder.with(context, "test", statements);
    }

    public void leaseEntity(String tpId, String leaseHolder) {
        try (var conn = connectionSupplier.get()) {
            leaseContextBuilder.by(leaseHolder).withConnection(conn).acquireLease(tpId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void leaseEntity(String tpId, String leaseHolder, Duration leaseDuration) {
        try (var conn = connectionSupplier.get()) {
            leaseContextBuilder.by(leaseHolder).forTime(leaseDuration).withConnection(conn).acquireLease(tpId);
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
