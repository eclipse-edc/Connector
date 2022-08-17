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

package org.eclipse.dataspaceconnector.dataplane.framework.store;

import org.eclipse.dataspaceconnector.common.collection.LruCache;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements an in-memory, ephemeral store with a maximum capacity. If the store grows beyond capacity, the oldest entry will be evicted.
 */
public class InMemoryDataPlaneStore implements DataPlaneStore {
    private final LruCache<String, State> cache;
    private final LockManager lockManager;

    public InMemoryDataPlaneStore(int capacity) {
        cache = new LruCache<>(capacity);
        lockManager = new LockManager(new ReentrantReadWriteLock());
    }

    @Override
    public void received(String processId) {
        lockManager.writeLock(() -> cache.put(processId, State.RECEIVED));
    }

    @Override
    public void completed(String processId) {
        lockManager.writeLock(() -> cache.put(processId, State.COMPLETED));
    }

    @Override
    public State getState(String processId) {
        return lockManager.readLock(() -> cache.getOrDefault(processId, State.NOT_TRACKED));
    }

}
