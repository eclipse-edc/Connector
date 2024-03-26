/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.core.edr.defaults;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In memory implementation of {@link EndpointDataReferenceEntryIndex}
 */
public class InMemoryEndpointDataReferenceEntryIndex implements EndpointDataReferenceEntryIndex {

    private final QueryResolver<EndpointDataReferenceEntry> queryResolver;
    private final Map<String, EndpointDataReferenceEntry> cache = new ConcurrentHashMap<>();

    public InMemoryEndpointDataReferenceEntryIndex(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(EndpointDataReferenceEntry.class, criterionOperatorRegistry);
    }

    @Override
    public @Nullable EndpointDataReferenceEntry findById(String transferProcessId) {
        return cache.get(transferProcessId);
    }

    @Override
    public StoreResult<List<EndpointDataReferenceEntry>> query(QuerySpec spec) {
        return StoreResult.success(queryResolver.query(cache.values().stream(), spec).collect(Collectors.toList()));
    }

    @Override
    public StoreResult<Void> save(EndpointDataReferenceEntry entry) {
        cache.put(entry.getTransferProcessId(), entry);
        return StoreResult.success();
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> delete(String transferProcessId) {
        return Optional.ofNullable(cache.remove(transferProcessId))
                .map(StoreResult::success)
                .orElse(StoreResult.notFound("EDR entry not found for transfer process %s".formatted(transferProcessId)));
    }
}
