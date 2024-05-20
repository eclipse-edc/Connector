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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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
    public ServiceResult<List<DataPlaneInstance>> getAll() {
        return transactionContext.execute(() -> {
            try (var stream = store.getAll()) {
                return ServiceResult.success(stream.toList());
            }
        });
    }

    @Override
    public ServiceResult<DataPlaneInstance> select(DataAddress source, String transferType, @Nullable String selectionStrategy) {
        var sanitizedSelectionStrategy = Optional.ofNullable(selectionStrategy).orElse(DEFAULT_STRATEGY);
        var strategy = selectionStrategyRegistry.find(sanitizedSelectionStrategy);
        if (strategy == null) {
            return ServiceResult.badRequest("Strategy " + sanitizedSelectionStrategy + " was not found");
        }

        return transactionContext.execute(() -> {
            try (var stream = store.getAll()) {
                var dataPlanes = stream.filter(dataPlane -> dataPlane.canHandle(source, transferType)).toList();
                var dataPlane = strategy.apply(dataPlanes);
                if (dataPlane == null) {
                    return ServiceResult.notFound("DataPlane not found");
                }
                return ServiceResult.success(dataPlane);
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

    @Override
    public ServiceResult<Void> delete(String instanceId) {
        return transactionContext.execute(() -> ServiceResult.from(store.deleteById(instanceId))).mapEmpty();
    }
}
