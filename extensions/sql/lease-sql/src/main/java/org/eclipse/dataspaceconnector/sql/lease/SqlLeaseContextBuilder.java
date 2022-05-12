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
import java.time.Duration;
import java.util.Objects;

/**
 * Utility class to assemble a {@link SqlLeaseContext} and execute operations on it using a {@link Connection}.
 * The intended use is to create the {@linkplain SqlLeaseContextBuilder} once and set the connection many times:
 * <pre>
 *  //in ctor:
 *  lc = SqlLeaseContextconfiguration.with( .... );
 *
 *  //in method 1
 *  void method1(){
 *      var connection = ...;
 *      lc.withConnection(connection).acquireLease(...);
 *      //or
 *      lc.withConnection(connection).breakLease(...);
 *  }
 * </pre>
 * <p>
 * Please do NOT keep references to the {@link SqlLeaseContext}, as this would keep the underlying connection open!
 */
public class SqlLeaseContextBuilder {
    private final TransactionContext trxContext;
    private final LeaseStatements statements;
    private String leaseHolder;
    private Duration leaseDuration;

    private SqlLeaseContextBuilder(TransactionContext trxContext, LeaseStatements statements, String leaseHolder) {
        this.trxContext = trxContext;
        this.statements = statements;
        this.leaseHolder = leaseHolder;
    }

    public static SqlLeaseContextBuilder with(TransactionContext trxContext, String leaseHolder, LeaseStatements statements) {
        Objects.requireNonNull(trxContext, "trxContext");
        Objects.requireNonNull(leaseHolder, "leaseHolder");
        Objects.requireNonNull(statements, "statements");
        return new SqlLeaseContextBuilder(trxContext, statements, leaseHolder);
    }

    /**
     * Sets the name which is used when acquiring a lease.
     */
    public SqlLeaseContextBuilder by(String leaseHolder) {
        this.leaseHolder = leaseHolder;
        return this;
    }

    /**
     * configures the duration for which the lease is acquired. Has no effect when breaking or getting the lease
     */
    public SqlLeaseContextBuilder forTime(Duration duration) {
        leaseDuration = duration;
        return this;
    }

    /**
     * sets the {@linkplain Connection} on which the next DB statement is executed.<p>
     * <strong>Storing references to the {@link SqlLeaseContext} is strongly discouraged, as this would keep the database {@link Connection} open!</strong>
     */
    public SqlLeaseContext withConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");
        return new SqlLeaseContext(trxContext, statements, leaseHolder, leaseDuration, connection);
    }
}
