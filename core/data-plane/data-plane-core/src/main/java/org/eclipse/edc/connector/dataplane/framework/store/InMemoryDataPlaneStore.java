/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework.store;

import org.eclipse.edc.connector.core.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements an in-memory, ephemeral store with a maximum capacity. If the store grows beyond capacity, the oldest entry will be evicted.
 */
public class InMemoryDataPlaneStore implements DataPlaneStore {

    private final InMemoryStatefulEntityStore<DataFlow> store;

    public InMemoryDataPlaneStore() {
        this(UUID.randomUUID().toString(), new HashMap<>());
    }

    public InMemoryDataPlaneStore(String connectorName, Map<String, Lease> leases) {
        store = new InMemoryStatefulEntityStore<>(DataFlow.class, connectorName, Clock.systemUTC(), leases);
    }

    @Override
    public @Nullable DataFlow findById(String id) {
        return store.find(id);
    }

    @Override
    public @NotNull List<DataFlow> nextNotLeased(int max, Criterion... criteria) {
        return store.leaseAndGet(max, criteria);
    }

    @Override
    public StoreResult<DataFlow> findByIdAndLease(String id) {
        return store.leaseAndGet(id);
    }

    @Override
    public void save(DataFlow entity) {
        store.upsert(entity);
    }
}
