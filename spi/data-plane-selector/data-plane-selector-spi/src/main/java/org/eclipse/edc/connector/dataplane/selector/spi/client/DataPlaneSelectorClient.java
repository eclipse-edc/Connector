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

package org.eclipse.edc.connector.dataplane.selector.spi.client;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.strategy.SelectionStrategy;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF selector.
 */
@ExtensionPoint
public interface DataPlaneSelectorClient {

    /**
     * Returns all {@link DataPlaneInstance}s known in the system
     */
    List<DataPlaneInstance> getAll();

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the default
     * {@link SelectionStrategy} which is "random"
     */
    @Nullable
    DataPlaneInstance find(DataAddress source, DataAddress destination);

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the given
     * {@link SelectionStrategy}.
     *
     * @throws IllegalArgumentException if the selection strategy was not found
     */
    @Nullable
    DataPlaneInstance find(DataAddress source, DataAddress destination, String selectionStrategyName);

}
