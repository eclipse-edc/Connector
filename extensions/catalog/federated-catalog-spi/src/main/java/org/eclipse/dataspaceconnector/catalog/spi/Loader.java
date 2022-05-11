/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.spi;

import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

/**
 * Puts the result of a catalog request (i.e. {@link org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse} into
 * whatever storage backend or database is used.
 */
public interface Loader {
    /**
     * Stores a list of {@link UpdateResponse}s in its internal database.
     */
    void load(Collection<UpdateResponse> batch);

    /**
     * Deletes all stored entries. Typically called before loading a new batch.
     */
    void clear();
}
