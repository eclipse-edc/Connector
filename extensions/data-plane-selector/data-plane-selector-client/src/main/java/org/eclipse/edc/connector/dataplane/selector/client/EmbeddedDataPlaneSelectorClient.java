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

package org.eclipse.edc.connector.dataplane.selector.client;

import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;

/**
 * Implementation of a {@link DataPlaneSelectorClient} that uses a local {@link DataPlaneSelector},
 * i.e. one that runs in the same JVM as the control plane.
 */
public class EmbeddedDataPlaneSelectorClient implements DataPlaneSelectorClient {


    private final DataPlaneSelectorService selector;

    public EmbeddedDataPlaneSelectorClient(DataPlaneSelectorService selector) {
        this.selector = selector;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        return selector.getAll();
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination) {
        return selector.select(source, destination);
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination, String selectionStrategyName) {
        return selector.select(source, destination, selectionStrategyName);
    }
}
