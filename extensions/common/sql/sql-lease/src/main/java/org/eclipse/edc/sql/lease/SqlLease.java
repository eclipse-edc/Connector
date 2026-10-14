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

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.spi.persistence.Lease;

import java.time.Clock;

/**
 * SQL-based implementation of a {@linkplain Lease}. Adds the {@code leaseId} property for use with RDBMS (PK, index,...)
 *
 * @deprecated the lease is now stored on the entity row (see {@code org.eclipse.edc.sql.lease.StatefulEntityStatements}), this will be removed in a future release.
 */
@Deprecated(since = "1.0.0")
public class SqlLease extends Lease {
    private final String resourceKind;
    private final String resourceId;

    public SqlLease(String leasedBy, String resourceId, String resourceKind, long leasedAt, long leaseDurationMillis) {
        super(leasedBy, leasedAt, leaseDurationMillis);
        this.resourceId = resourceId;
        this.resourceKind = resourceKind;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceKind() {
        return resourceKind;
    }

    public boolean isExpired(Clock clock) {
        return getLeasedAt() + getLeaseDuration() < clock.millis();
    }
}
