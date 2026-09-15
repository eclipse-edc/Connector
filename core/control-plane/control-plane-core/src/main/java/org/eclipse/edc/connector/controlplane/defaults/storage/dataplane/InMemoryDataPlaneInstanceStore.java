/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.defaults.storage.dataplane;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Default (=in-memory) implementation for the {@link DataPlaneInstanceStore}.
 */
public class InMemoryDataPlaneInstanceStore implements DataPlaneInstanceStore {

    private final Map<String, DataPlaneInstance> dataplaneInstances = new ConcurrentHashMap<>();
    private final QueryResolver<DataPlaneInstance> queryResolver;

    public InMemoryDataPlaneInstanceStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        queryResolver = new ReflectionBasedQueryResolver<>(DataPlaneInstance.class, criterionOperatorRegistry);
    }

    @Override
    public @Nullable DataPlaneInstance findById(String id) {
        return dataplaneInstances.get(id);
    }

    @Override
    public StoreResult<Void> save(DataPlaneInstance entity) {
        dataplaneInstances.put(entity.getId(), entity);
        return StoreResult.success();
    }

    @Override
    public StoreResult<DataPlaneInstance> deleteById(String instanceId) {
        var old = dataplaneInstances.remove(instanceId);
        if (old == null) {
            return StoreResult.notFound("Data plane instance %s not found".formatted(instanceId));
        }
        return StoreResult.success(old);
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        return dataplaneInstances.values().stream();
    }

    @Override
    public Stream<DataPlaneInstance> query(QuerySpec querySpec) {
        return queryResolver.query(dataplaneInstances.values().stream(), querySpec);
    }
}
