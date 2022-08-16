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

package org.eclipse.dataspaceconnector.dataplane.selector.client;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF selector.
 */
public interface DataPlaneSelectorClient {

    /**
     * Returns all {@link DataPlaneInstance}s known in the system
     */
    List<DataPlaneInstance> getAll();

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the default
     * {@link org.eclipse.dataspaceconnector.dataplane.selector.strategy.SelectionStrategy} which is "random"
     */
    @Nullable
    DataPlaneInstance find(DataAddress source, DataAddress destination);

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the given
     * {@link org.eclipse.dataspaceconnector.dataplane.selector.strategy.SelectionStrategy}.
     *
     * @throws IllegalArgumentException if the selection strategy was not found
     */
    @Nullable
    DataPlaneInstance find(DataAddress source, DataAddress destination, String selectionStrategyName);

}
