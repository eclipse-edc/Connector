/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.services.dataplane;

import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.controlplane.dataplane.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.controlplane.dataplane.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;

import static org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstanceStates.UNREGISTERED;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

public class DataPlaneSelectorServiceImpl implements DataPlaneSelectorService {

    private final DataPlaneInstanceStore store;
    private final SelectionStrategyRegistry selectionStrategyRegistry;
    private final TransactionContext transactionContext;
    private final String selectionStrategy;

    public DataPlaneSelectorServiceImpl(DataPlaneInstanceStore store, SelectionStrategyRegistry selectionStrategyRegistry,
                                        TransactionContext transactionContext, String selectionStrategy) {
        this.store = store;
        this.selectionStrategyRegistry = selectionStrategyRegistry;
        this.transactionContext = transactionContext;
        this.selectionStrategy = selectionStrategy;
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
    public ServiceResult<List<DataPlaneInstance>> search(QuerySpec querySpec) {
        return transactionContext.execute(() -> {
            try (var stream = store.query(querySpec)) {
                return ServiceResult.success(stream.toList());
            }
        });
    }

    @Override
    public ServiceResult<DataPlaneInstance> selectFor(TransferProcess transferProcess) {
        var strategy = selectionStrategyRegistry.find(selectionStrategy);
        if (strategy == null) {
            return ServiceResult.badRequest("Strategy " + selectionStrategy + " was not found");
        }
        return transactionContext.execute(() -> {
            try (var stream = store.query(queryByParticipantContextId(transferProcess.getParticipantContextId()).build())) {
                var dataPlanes = stream
                        .filter(it -> it.getState() != UNREGISTERED.code())
                        .filter(it -> it.getAllowedTransferTypes().contains(transferProcess.getTransferType()))
                        .filter(it -> {
                            var transferLabels = transferProcess.getDataplaneMetadata().getLabels();
                            return transferLabels.isEmpty() || it.getLabels().containsAll(transferLabels);
                        })
                        .toList();

                if (dataPlanes.isEmpty()) {
                    return ServiceResult.notFound("No dataplane found");
                }

                var dataPlane = strategy.apply(dataPlanes);
                if (dataPlane == null) {
                    return ServiceResult.notFound(selectionStrategy + " strategy failed to select a dataplane");
                }

                return ServiceResult.success(dataPlane);
            }
        });
    }

    @Override
    public ServiceResult<Void> register(DataPlaneInstance instance) {
        return transactionContext.execute(() -> {
            instance.transitionToRegistered();
            return store.save(instance).flatMap(ServiceResult::from);
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
