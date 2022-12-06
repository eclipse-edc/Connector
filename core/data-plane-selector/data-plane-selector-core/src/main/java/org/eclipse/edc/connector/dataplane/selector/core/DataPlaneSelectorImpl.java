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

package org.eclipse.edc.connector.dataplane.selector.core;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.stream.Collectors;

public class DataPlaneSelectorImpl implements DataPlaneSelector {

    private final DataPlaneInstanceStore instanceStore;

    public DataPlaneSelectorImpl(DataPlaneInstanceStore instanceStore) {
        this.instanceStore = instanceStore;
    }

    @Override
    public DataPlaneInstance select(DataAddress sourceAddress, DataAddress destinationAddress, SelectionStrategy strategy) {
        return strategy.apply(instanceStore.getAll().filter(di -> di.canHandle(sourceAddress, destinationAddress)).collect(Collectors.toList()));
    }
}
