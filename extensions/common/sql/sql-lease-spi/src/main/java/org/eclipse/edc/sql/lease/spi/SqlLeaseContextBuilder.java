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

/**
 * Builder for creating a {@link LeaseContext} for SQL-based leases.
 */
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
}
