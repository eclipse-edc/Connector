/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.metadata.memory;

import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An ephemeral metadata store.
 */
public class InMemoryMetadataStore implements MetadataStore {
    private Map<String, DataEntry<?>> cache = new ConcurrentHashMap<>();

    @Override
    public @Nullable DataEntry<?> findForId(String id) {
        return cache.get(id);
    }

    @Override
    public void save(DataEntry<?> entry) {
        cache.put(entry.getId(), entry);
    }
}
