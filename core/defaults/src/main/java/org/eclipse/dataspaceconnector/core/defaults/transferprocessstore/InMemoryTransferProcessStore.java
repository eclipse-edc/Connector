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

package org.eclipse.dataspaceconnector.core.defaults.transferprocessstore;

import org.eclipse.dataspaceconnector.core.defaults.InMemoryStatefulEntityStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * An in-memory, threadsafe process store.
 * This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore implements TransferProcessStore {

    private final InMemoryStatefulEntityStore<TransferProcess> store = new InMemoryStatefulEntityStore<>(TransferProcess.class);

    @Override
    @Nullable
    public String processIdForTransferId(String id) {
        return store.findAll()
                .filter(p -> id.equals(p.getDataRequest().getId()))
                .findFirst()
                .map(TransferProcess::getId)
                .orElse(null);
    }

    @Override
    public void create(TransferProcess process) {
        store.upsert(process);
    }

    @Override
    public void update(TransferProcess process) {
        store.upsert(process);
    }

    @Nullable
    @Override
    public TransferProcess find(String id) {
        return store.find(id);
    }

    @Override
    public void delete(String id) {
        store.delete(id);
    }

    @Override
    public Stream<TransferProcess> findAll(QuerySpec querySpec) {
        return store.findAll(querySpec);
    }

    @Override
    public @NotNull List<TransferProcess> nextForState(int state, int max) {
        return store.nextForState(state, max);
    }

    public Stream<TransferProcess> findAll() {
        return store.findAll();
    }
}
