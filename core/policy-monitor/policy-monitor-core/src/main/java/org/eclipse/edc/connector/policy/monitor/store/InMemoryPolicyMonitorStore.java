/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.policy.monitor.store;

import org.eclipse.edc.connector.core.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * In-memory implementation of the {@link PolicyMonitorStore}
 */
public class InMemoryPolicyMonitorStore implements PolicyMonitorStore {

    private final InMemoryStatefulEntityStore<PolicyMonitorEntry> store;

    public InMemoryPolicyMonitorStore() {
        store = new InMemoryStatefulEntityStore<>(PolicyMonitorEntry.class, UUID.randomUUID().toString(), Clock.systemUTC(), new HashMap<>());
    }

    @Override
    public @Nullable PolicyMonitorEntry findById(String id) {
        return store.find(id);
    }

    @Override
    public @NotNull List<PolicyMonitorEntry> nextNotLeased(int max, Criterion... criteria) {
        return store.leaseAndGet(max, criteria);
    }

    @Override
    public StoreResult<PolicyMonitorEntry> findByIdAndLease(String id) {
        return store.leaseAndGet(id);
    }

    @Override
    public void save(PolicyMonitorEntry entity) {
        store.upsert(entity);
    }
}
