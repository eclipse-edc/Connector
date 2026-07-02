/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.profile;

import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DataspaceProfileServiceImpl implements DataspaceProfileService {

    private final TransactionContext transactionContext;
    private final DataspaceProfileStore store;
    private final DataspaceProfileContextRegistry registry;
    private final DataspaceProfileMapper mapper;

    DataspaceProfileServiceImpl(TransactionContext transactionContext, DataspaceProfileStore store,
                                DataspaceProfileContextRegistry registry, DataspaceProfileMapper mapper) {
        this.transactionContext = transactionContext;
        this.store = store;
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public DataspaceProfile findById(String name) {
        return transactionContext.execute(() -> store.findById(name));
    }

    @Override
    public ServiceResult<List<DataspaceProfile>> search(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.findAll(query)) {
                return ServiceResult.success(stream.toList());
            }
        });
    }

    @Override
    public @NotNull ServiceResult<DataspaceProfile> create(DataspaceProfile profile) {
        return transactionContext.execute(() -> {
            var result = store.create(profile);
            result.onSuccess(p -> registry.register(mapper.toContext(p)));
            return ServiceResult.from(result);
        });
    }

    @Override
    public @NotNull ServiceResult<DataspaceProfile> deleteById(String name) {
        return transactionContext.execute(() -> ServiceResult.from(store.delete(name)));
    }
}
