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

/**
 * Provides a builder for creating {@link org.eclipse.edc.spi.persistence.LeaseContext} instances for a specific resource kind.
 */
public interface SqlLeaseContextBuilderProvider {
    
    /**
     * Creates a new {@link SqlLeaseContextBuilder} for the specified resource kind.
     *
     * @param resourceKind the kind of resource to be leased
     * @return a new {@link SqlLeaseContextBuilder} instance
     */
    SqlLeaseContextBuilder createContextBuilder(String resourceKind);

    /**
     * Gets the SQL statements implementation used by this provider.
     *
     * @return the {@link LeaseStatements} instance
     */
    LeaseStatements getStatements();
}
