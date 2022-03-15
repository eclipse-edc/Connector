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

import org.eclipse.dataspaceconnector.spi.persistence.Lease;

import java.time.Instant;

/**
 * SQL-based implementation of a {@linkplain Lease}. Adds the {@code leaseId} property for use with RDBMS (PK, index,...)
 */
public class SqlLease extends Lease {
    private String leaseId;

    public SqlLease(String leasedBy, long leasedAt, long leaseDurationMillis) {
        super(leasedBy, leasedAt, leaseDurationMillis);
    }

    public String getLeaseId() {
        return leaseId;
    }

    public void setLeaseId(String leaseId) {
        this.leaseId = leaseId;
    }

    public boolean isExpired() {
        return getLeasedAt() + getLeaseDuration() < Instant.now().toEpochMilli();
    }
}
