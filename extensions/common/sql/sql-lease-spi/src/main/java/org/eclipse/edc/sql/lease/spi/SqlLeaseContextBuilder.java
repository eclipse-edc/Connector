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

package org.eclipse.edc.sql.lease.spi;

import org.eclipse.edc.spi.persistence.LeaseContext;

import java.sql.Connection;
import java.time.Duration;

/**
 * Builder for creating a {@link LeaseContext} for SQL-based leases.
 *
 * @deprecated the lease is now stored on the entity row (see {@code org.eclipse.edc.sql.lease.StatefulEntityStatements}), this will be removed in a future release.
 */
@Deprecated(since = "1.0.0")
public interface SqlLeaseContextBuilder {
    /**
     * Sets the SQL connection to be used for lease operations.
     *
     * @param connection the SQL connection
     * @return the {@link LeaseContext} instance
     */
    LeaseContext withConnection(Connection connection);

    /**
     * Sets the identifier of the entity leasing the resource.
     *
     * @param leasedBy the identifier of the entity leasing the resource
     * @return the {@link SqlLeaseContextBuilder} instance
     */
    SqlLeaseContextBuilder by(String leasedBy);

    /**
     * Sets the duration for which a lease is acquired. Without it, a lease lasts 60 seconds. A lease must last longer than
     * the work that is done under it, e.g. the processing of a batch of entities by a state machine, otherwise another
     * runtime may lease the same entities while they are still being processed.
     *
     * @param duration the duration of a lease
     * @return the {@link SqlLeaseContextBuilder} instance
     */
    SqlLeaseContextBuilder forTime(Duration duration);
}
