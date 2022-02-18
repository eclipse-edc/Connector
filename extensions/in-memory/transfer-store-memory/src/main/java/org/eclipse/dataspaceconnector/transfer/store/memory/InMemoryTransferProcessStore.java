/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore implements TransferProcessStore {
    private final LockManager lockManager = new LockManager(new ReentrantReadWriteLock());
    private final Map<String, TransferProcess> processesById = new HashMap<>();
    private final Map<String, TransferProcess> processesByExternalId = new HashMap<>();
    private final Map<Integer, List<TransferProcess>> stateCache = new HashMap<>();

    @Override
    public TransferProcess find(String id) {
        return lockManager.readLock(() -> processesById.get(id));
    }

    @Override
    @Nullable
    public String processIdForTransferId(String id) {
        var process = processesByExternalId.get(id);
        return process != null ? process.getId() : null;
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        return lockManager.readLock(() -> {
            var set = stateCache.get(state);
            return set == null ? Collections.emptyList() : set.stream()
                    .sorted(Comparator.comparingLong(TransferProcess::getStateTimestamp)) //order by state timestamp, oldest first
                    .limit(max)
                    .map(TransferProcess::copy)
                    .collect(toList());
        });
    }

    @Override
    public void create(TransferProcess process) {
        lockManager.writeLock(() -> {
            delete(process.getId());
            TransferProcess internalCopy = process.copy();
            processesById.put(process.getId(), internalCopy);
            processesByExternalId.put(process.getDataRequest().getId(), internalCopy);
            stateCache.computeIfAbsent(process.getState(), k -> new ArrayList<>()).add(internalCopy);
            return null;
        });
    }

    @Override
    public void update(TransferProcess process) {
        lockManager.writeLock(() -> {
            process.updateStateTimestamp();
            delete(process.getId());
            TransferProcess internalCopy = process.copy();
            processesByExternalId.put(process.getDataRequest().getId(), internalCopy);
            processesById.put(process.getId(), internalCopy);
            stateCache.computeIfAbsent(process.getState(), k -> new ArrayList<>()).add(internalCopy);
            return null;
        });
    }

    @Override
    public void delete(String processId) {
        lockManager.writeLock(() -> {
            TransferProcess process = processesById.remove(processId);
            if (process != null) {
                var tempCache = new HashMap<Integer, List<TransferProcess>>();
                stateCache.forEach((key, value) -> {
                    var list = value.stream().filter(p -> !p.getId().equals(processId)).collect(Collectors.toCollection(ArrayList::new));
                    tempCache.put(key, list);
                });
                stateCache.clear();
                stateCache.putAll(tempCache);
                processesByExternalId.remove(process.getDataRequest().getId());
            }
            return null;
        });
    }

    @Override
    public void createData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void updateData(String processId, String key, Object data) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, String key) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deleteData(String processId, Set<String> keys) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
