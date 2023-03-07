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

package org.eclipse.edc.connector.dataplane.selector.store;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Default (=in-memory) implementation for the {@link DataPlaneInstanceStore}. All r/w access is secured with a {@link LockManager}.
 */
public class InMemoryDataPlaneInstanceStore implements DataPlaneInstanceStore {

    private final Map<String, DataPlaneInstance> instances = new ConcurrentHashMap<>();

    public InMemoryDataPlaneInstanceStore() {
    }

    @Override
    public StoreResult<Void> create(DataPlaneInstance instance) {
        var prev = instances.putIfAbsent(instance.getId(), instance);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>alreadyExists(format(DATA_PLANE_INSTANCE_EXISTS, instance.getId())))
                .orElse(StoreResult.success());

    }

    @Override
    public StoreResult<Void> update(DataPlaneInstance instance) {
        var prev = instances.replace(instance.getId(), instance);
        return Optional.ofNullable(prev)
                .map(a -> StoreResult.<Void>success())
                .orElse(StoreResult.notFound(format(DATA_PLANE_INSTANCE_NOT_FOUND, instance.getId())));
    }

    @Override
    public DataPlaneInstance findById(String id) {
        return instances.get(id);
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        return instances.values().stream();
    }
}
