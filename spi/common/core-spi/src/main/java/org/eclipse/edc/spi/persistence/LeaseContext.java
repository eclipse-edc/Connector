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

package org.eclipse.edc.spi.persistence;

import org.eclipse.edc.spi.result.StoreResult;

/**
 * Interface for storage implementations that need to "lease" certain entities, i.e. block them from subsequent
 * read/write access.
 */
public interface LeaseContext {

    /**
     * Breaks the exclusive Lock on an entity
     *
     * @param entityId The database ID of the entity
     * @return a {@link StoreResult} indicating success or failure. Failure can happen if another holder holds the lease
     */
    StoreResult<Void> breakLease(String entityId);

    /**
     * Acquires the exclusive Lock on an entity
     *
     * @param entityId The database ID of the entity
     * @return a {@link StoreResult} indicating success or failure. Failure can happen if another holder already holds the lease.
     */
    StoreResult<Void> acquireLease(String entityId);
}
