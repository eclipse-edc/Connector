/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategyRegistry;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.List;
import java.util.stream.Collectors;

public class EmbeddedDataPlaneSelectorService implements DataPlaneSelectorService {

    private final DataPlaneSelector selector;
    private final DataPlaneInstanceStore store;
    private final SelectionStrategyRegistry selectionStrategyRegistry;
    private final TransactionContext transactionContext;

    public EmbeddedDataPlaneSelectorService(DataPlaneSelector selector, DataPlaneInstanceStore store, SelectionStrategyRegistry selectionStrategyRegistry, TransactionContext transactionContext) {
        this.selector = selector;
        this.store = store;
        this.selectionStrategyRegistry = selectionStrategyRegistry;
        this.transactionContext = transactionContext;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        return store.getAll().collect(Collectors.toList());
    }

    @Override
    public DataPlaneInstance select(DataAddress source, DataAddress destination) {
        return selector.select(source, destination);
    }

    @Override
    public DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy) {
        var strategy = selectionStrategyRegistry.find(selectionStrategy);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy " + selectionStrategy + " was not found");
        }
        return selector.select(source, destination, strategy);
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
