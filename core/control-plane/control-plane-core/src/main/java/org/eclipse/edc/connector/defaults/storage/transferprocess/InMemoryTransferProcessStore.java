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

package org.eclipse.edc.connector.defaults.storage.transferprocess;

import org.eclipse.edc.connector.defaults.storage.InMemoryStatefulEntityStore;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.persistence.Lease;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore implements TransferProcessStore {

    private final InMemoryStatefulEntityStore<TransferProcess> store;

    public InMemoryTransferProcessStore() {
        this(UUID.randomUUID().toString(), Clock.systemUTC(), new HashMap<>());
    }

    public InMemoryTransferProcessStore(String leaserId, Clock clock, Map<String, Lease> leases) {
        store = new InMemoryStatefulEntityStore<>(TransferProcess.class, leaserId, clock, leases);
    }

    @Nullable
    @Override
    public TransferProcess findById(String id) {
        return store.find(id);
    }

    @Override
    public @Nullable TransferProcess findForCorrelationId(String correlationId) {
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("dataRequest.id", "=", correlationId)).build();

        return store.findAll(querySpec).findFirst().orElse(null);
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
    public @NotNull List<TransferProcess> nextNotLeased(int max, Criterion... criteria) {
        return store.leaseAndGet(max, criteria);
    }

    @Override
    public StoreResult<TransferProcess> findByIdAndLease(String id) {
        return store.leaseAndGet(id);
    }

    @Override
    public StoreResult<TransferProcess> findByCorrelationIdAndLease(String correlationId) {
        var transferProcess = findForCorrelationId(correlationId);
        if (transferProcess == null) {
            return StoreResult.notFound(format("TransferProcess with correlationId %s not found", correlationId));
        }

        return findByIdAndLease(transferProcess.getId());
    }

    @Override
    public void save(TransferProcess entity) {
        store.upsert(entity);
    }

}
