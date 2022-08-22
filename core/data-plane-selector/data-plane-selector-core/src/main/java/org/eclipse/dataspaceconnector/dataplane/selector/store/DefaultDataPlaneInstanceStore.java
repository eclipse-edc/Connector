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

package org.eclipse.dataspaceconnector.dataplane.selector.store;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Default (=in-memory) implementation for the {@link DataPlaneInstanceStore}. All r/w access is secured with a {@link LockManager}.
 */
public class DefaultDataPlaneInstanceStore implements DataPlaneInstanceStore {

    private final LockManager lockManager;
    private final List<DataPlaneInstance> list;

    public DefaultDataPlaneInstanceStore() {
        lockManager = new LockManager(new ReentrantReadWriteLock(true));
        list = new ArrayList<>();
    }

    @Override
    public void save(DataPlaneInstance instance) {
        lockManager.writeLock(() -> {
            list.removeIf(i -> Objects.equals(i.getId(), instance.getId()));
            return list.add(instance);
        });
    }

    @Override
    public void saveAll(Collection<DataPlaneInstance> instances) {
        lockManager.writeLock(() -> list.addAll(instances));
    }

    @Override
    public DataPlaneInstance findById(String id) {
        return lockManager.readLock(() -> list.stream().filter(dpi -> Objects.equals(dpi.getId(), id)).findFirst().orElse(null));
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        return lockManager.readLock(list::stream);
    }
}
