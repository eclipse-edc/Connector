/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.metadata;

import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import org.jetbrains.annotations.Nullable;

/**
 * Delegates to a metadata storage system for the persistence and management of data entries.
 */
public interface MetadataStore {

    /**
     * Returns the entry for the id or null if not found.
     */
    @Nullable
    DataEntry<?> findForId(String id);

    /**
     * Saves an entry to the backing store.
     */
    void save(DataEntry<?> entry);
}
