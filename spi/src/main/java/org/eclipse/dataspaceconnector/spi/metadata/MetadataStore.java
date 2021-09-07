/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.metadata;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Delegates to a metadata storage system for the persistence and management of data entries.
 *
 */
@Deprecated
// TODO MetadataStore is replaced by AssetIndex
public interface MetadataStore {

    /**
     * Returns the entry for the id or null if not found.
     */
    @Nullable
    DataEntry findForId(String id);

    /**
     * Saves an entry to the backing store.
     */
    void save(DataEntry entry);

    /**
     * Returns all data entries that match the given set of policies
     */
    @NotNull
    Collection<DataEntry> queryAll(Collection<Policy> policies);
}
