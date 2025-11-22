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
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates.AVAILABLE;

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
    public ServiceResult<DataPlaneInstance> select(@Nullable String selectionStrategy, Predicate<DataPlaneInstance> filter) {
        var sanitizedSelectionStrategy = Optional.ofNullable(selectionStrategy).orElse(DEFAULT_STRATEGY);
        var strategy = selectionStrategyRegistry.find(sanitizedSelectionStrategy);
        if (strategy == null) {
            return ServiceResult.badRequest("Strategy " + sanitizedSelectionStrategy + " was not found");
        }
        return transactionContext.execute(() -> {
            try (var stream = store.getAll()) {
                var dataPlanes = stream
                        .filter(it -> it.getState() == AVAILABLE.code())
                        .filter(filter)
                        .toList();

                if (dataPlanes.isEmpty()) {
                    return ServiceResult.notFound("No dataplane found");
                }

                var dataPlane = strategy.apply(dataPlanes);
                return ServiceResult.success(dataPlane);
            }
        });
    }

    @Override
    public ServiceResult<Void> register(DataPlaneInstance instance) {
        return transactionContext.execute(() -> {
            instance.transitionToRegistered();
            store.save(instance);
            return ServiceResult.success();
        });
    }

    @Override
    public ServiceResult<Void> delete(String instanceId) {
        return transactionContext.execute(() -> ServiceResult.from(store.deleteById(instanceId))).mapEmpty();
    }

    @Override
    public ServiceResult<Void> unregister(String instanceId) {
        return transactionContext.execute(() -> {
            StoreResult<Void> operation = store.findByIdAndLease(instanceId)
                    .map(it -> {
                        it.transitionToUnregistered();
                        store.save(it);
                        return null;
                    });

            return ServiceResult.from(operation);
        });
    }

    @Override
    public ServiceResult<Void> update(DataPlaneInstance instance) {
        return transactionContext.execute(() -> store.findByIdAndLease(instance.getId())
                .map(stored -> stored.toBuilder()
                        .url(instance.getUrl())
                        .allowedTransferType(instance.getAllowedTransferTypes())
                        .build())
                .compose(store::save)
                .flatMap(ServiceResult::from));
    }

    @Override
    public ServiceResult<DataPlaneInstance> findById(String id) {
        return transactionContext.execute(() -> {
            var instance = store.findById(id);
            if (instance == null) {
                return ServiceResult.notFound("Data Plane instance with id %s not found".formatted(id));
            }
            return ServiceResult.success(instance);
        });
    }
}
