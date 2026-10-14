/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilder;
import org.eclipse.edc.sql.lease.spi.SqlLeaseContextBuilderProvider;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

/**
 * Default {@link SqlLeaseContextBuilderProvider}, which stores leases in the {@code edc_lease} table.
 *
 * @deprecated the lease is now stored on the entity row (see {@code org.eclipse.edc.sql.lease.StatefulEntityStatements}), this will be removed in a future release.
 */
@Deprecated(since = "1.0.0")
public record SqlLeaseContextBuilderProviderImpl(TransactionContext transactionContext, LeaseStatements leaseStatements,
                                                 String leaseHolder, Clock clock,
                                                 QueryExecutor queryExecutor) implements SqlLeaseContextBuilderProvider {

    @Override
    public SqlLeaseContextBuilder createContextBuilder(String resourceKind) {
        return SqlLeaseContextBuilderImpl.with(transactionContext, leaseHolder, resourceKind, leaseStatements, clock, queryExecutor);
    }

}
