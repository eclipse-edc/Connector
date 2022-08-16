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

package org.eclipse.dataspaceconnector.spi.persistence;

/**
 * Interface for storage implementations that need to "lease" certain entities, i.e. block them from subsequent
 * read/write access.
 */
public interface LeaseContext {
    /**
     * Breaks the exclusive Lock on an entity
     *
     * @param entityId The database ID of the entity
     * @throws RuntimeException or subclass if the lease could not be broken, e.g. because another holder holds it.
     */
    void breakLease(String entityId);

    /**
     * Acquires the exclusive Lock on an entity
     *
     * @param entityId The database ID of the entity
     * @throws RuntimeException or subclass if the lease could not be acquired, e.g. because another holder holds it.
     */
    void acquireLease(String entityId);
}
