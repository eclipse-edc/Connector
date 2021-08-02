/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.metadata;

import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Delegates to a metadata storage system for the persistence and management of data entries.
 */
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
