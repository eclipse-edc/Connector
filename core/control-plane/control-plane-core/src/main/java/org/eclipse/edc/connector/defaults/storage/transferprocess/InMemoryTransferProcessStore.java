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

import org.eclipse.edc.connector.core.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.query.Criterion.criterion;

/**
 * An in-memory, threadsafe process store. This implementation is intended for testing purposes only.
 */
public class InMemoryTransferProcessStore extends InMemoryStatefulEntityStore<TransferProcess> implements TransferProcessStore {

    public InMemoryTransferProcessStore(Clock clock) {
        this(UUID.randomUUID().toString(), clock);
    }

    public InMemoryTransferProcessStore(String leaserId, Clock clock) {
        super(TransferProcess.class, leaserId, clock);
    }

    @Override
    public @Nullable TransferProcess findForCorrelationId(String correlationId) {
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion("dataRequest.id", "=", correlationId)).build();

        return super.findAll(querySpec).findFirst().orElse(null);
    }

    @Override
    public void delete(String id) {
        super.delete(id);
    }

    @Override
    public Stream<TransferProcess> findAll(QuerySpec querySpec) {
        return super.findAll(querySpec);
    }

    @Override
    public StoreResult<TransferProcess> findByCorrelationIdAndLease(String correlationId) {
        var transferProcess = findForCorrelationId(correlationId);
        if (transferProcess == null) {
            return StoreResult.notFound(format("TransferProcess with correlationId %s not found", correlationId));
        }

        return findByIdAndLease(transferProcess.getId());
    }

}
