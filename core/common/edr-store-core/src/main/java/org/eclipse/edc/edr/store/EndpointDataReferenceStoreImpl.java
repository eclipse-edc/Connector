/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.edr.store;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Default implementation of {@link EndpointDataReferenceStore}. It makes usage of two subcomponents
 * The metadata storage {@link EndpointDataReferenceEntryIndex} and the EDR cache {@link EndpointDataReferenceCache}
 * and coordinate the interaction between then for fulfilling the {@link EndpointDataReferenceStore} interface
 */
public class EndpointDataReferenceStoreImpl implements EndpointDataReferenceStore {

    private final EndpointDataReferenceEntryIndex dataReferenceEntryIndex;
    private final EndpointDataReferenceCache dataReferenceCache;
    private final TransactionContext transactionalContext;

    public EndpointDataReferenceStoreImpl(EndpointDataReferenceEntryIndex dataReferenceEntryIndex, EndpointDataReferenceCache dataReferenceCache, TransactionContext transactionalContext) {
        this.dataReferenceEntryIndex = dataReferenceEntryIndex;
        this.dataReferenceCache = dataReferenceCache;
        this.transactionalContext = transactionalContext;
    }

    @Override
    public StoreResult<DataAddress> resolveByTransferProcess(String transferProcessId) {
        return transactionalContext.execute(() -> dataReferenceCache.get(transferProcessId));
    }

    @Override
    public @Nullable EndpointDataReferenceEntry findById(String transferProcessId) {
        return transactionalContext.execute(() -> dataReferenceEntryIndex.findById(transferProcessId));
    }

    @Override
    public StoreResult<List<EndpointDataReferenceEntry>> query(QuerySpec querySpec) {
        return transactionalContext.execute(() -> dataReferenceEntryIndex.query(querySpec));
    }

    @Override
    public StoreResult<EndpointDataReferenceEntry> delete(String transferProcessId) {
        return transactionalContext.execute(() -> dataReferenceCache.delete(transferProcessId)
                .compose((i) -> dataReferenceEntryIndex.delete(transferProcessId)));
    }

    @Override
    public StoreResult<Void> save(EndpointDataReferenceEntry entry, DataAddress dataAddress) {
        return transactionalContext.execute(() -> dataReferenceCache.put(entry.getTransferProcessId(), dataAddress)
                .compose(i -> dataReferenceEntryIndex.save(entry)));
    }
}
