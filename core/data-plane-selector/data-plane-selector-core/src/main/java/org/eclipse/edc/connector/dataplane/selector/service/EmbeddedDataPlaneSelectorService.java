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

package org.eclipse.edc.connector.dataplane.selector.service;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

public class EmbeddedDataPlaneSelectorService implements DataPlaneSelectorService {

    private final DataPlaneInstanceStore store;
    private final SelectionStrategyRegistry selectionStrategyRegistry;
    private final TransactionContext transactionContext;

    public EmbeddedDataPlaneSelectorService(DataPlaneInstanceStore store, SelectionStrategyRegistry selectionStrategyRegistry, TransactionContext transactionContext) {
        this.store = store;
        this.selectionStrategyRegistry = selectionStrategyRegistry;
        this.transactionContext = transactionContext;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        return transactionContext.execute(() -> {
            try (var stream = store.getAll()) {
                return stream.toList();
            }
        });
    }

    @Override
    public DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy, String transferType) {
        var strategy = selectionStrategyRegistry.find(selectionStrategy);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy " + selectionStrategy + " was not found");
        }

        return transactionContext.execute(() -> {
            try (var stream = store.getAll()) {
                var dataPlanes = stream.filter(dataPlane -> dataPlane.canHandle(source, destination, transferType)).toList();
                return strategy.apply(dataPlanes);
            }
        });
    }


    @Override
    public ServiceResult<Void> addInstance(DataPlaneInstance instance) {
        return transactionContext.execute(() -> {
            StoreResult<Void> result;
            if (store.findById(instance.getId()) == null) {
                result = store.create(instance);
            } else {
                result = store.update(instance);
            }
            return ServiceResult.from(result);
        });
    }
}
