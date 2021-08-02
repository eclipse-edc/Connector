/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.metadata.memory;

import com.microsoft.dagx.policy.model.Identifiable;
import com.microsoft.dagx.policy.model.Policy;
import com.microsoft.dagx.spi.metadata.MetadataListener;
import com.microsoft.dagx.spi.metadata.MetadataObservable;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * An ephemeral metadata store.
 */
public class InMemoryMetadataStore extends MetadataObservable implements MetadataStore {
    private final Map<String, DataEntry> cache = new ConcurrentHashMap<>();

    @Override
    public @Nullable DataEntry findForId(String id) {
        getListeners().forEach(MetadataListener::searchInitiated);
        return cache.get(id);
    }

    @Override
    public void save(DataEntry entry) {
        if (cache.containsKey(entry.getId())) {
            getListeners().forEach(MetadataListener::metadataItemUpdated);
        } else {
            getListeners().forEach(MetadataListener::metadataItemAdded);
        }
        cache.put(entry.getId(), entry);
    }

    @Override
    public @NotNull Collection<DataEntry> queryAll(Collection<Policy> policies) {
        getListeners().forEach(MetadataListener::querySubmitted);
        Set<String> policyIds = policies.stream().map(Identifiable::getUid).collect(toSet());
        return cache.values().stream().filter(entry -> policyIds.contains(entry.getPolicyId())).collect(Collectors.toList());
    }
}
