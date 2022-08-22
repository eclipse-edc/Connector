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

package org.eclipse.dataspaceconnector.dataplane.selector;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.strategy.SelectionStrategyRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DataPlaneSelectorServiceImpl implements DataPlaneSelectorService {

    private final DataPlaneSelector selector;
    private final DataPlaneInstanceStore store;
    private final SelectionStrategyRegistry selectionStrategyRegistry;

    public DataPlaneSelectorServiceImpl(DataPlaneSelector selector, DataPlaneInstanceStore store, SelectionStrategyRegistry selectionStrategyRegistry) {
        this.selector = selector;
        this.store = store;
        this.selectionStrategyRegistry = selectionStrategyRegistry;
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
    public Collection<String> getAllStrategies() {
        return selectionStrategyRegistry.getAll();
    }

    @Override
    public void addInstance(DataPlaneInstance instance) {
        store.save(instance);
    }
}
