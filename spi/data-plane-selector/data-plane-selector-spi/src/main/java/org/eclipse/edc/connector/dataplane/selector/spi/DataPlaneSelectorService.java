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

package org.eclipse.edc.connector.dataplane.selector.spi;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF selector.
 */
@ExtensionPoint
public interface DataPlaneSelectorService {

    String DEFAULT_STRATEGY = "random";

    /**
     * Returns all {@link DataPlaneInstance}s known in the system
     */
    List<DataPlaneInstance> getAll();

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the configured
     * strategy.
     *
     * @deprecated please use the one that passes the transferType
     */
    @Deprecated(since = "0.6.3")
    default DataPlaneInstance select(DataAddress source, DataAddress destination) {
        return select(source, destination, "random");
    }

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the passed
     * strategy.
     *
     * @deprecated please use the one that passes the transferType
     */
    @Deprecated(since = "0.6.3")
    default DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy) {
        return select(source, destination, selectionStrategy, null);
    }

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the passed
     * strategy and the optional transferType.
     */
    DataPlaneInstance select(DataAddress source, DataAddress destination, @Nullable String selectionStrategy, @Nullable String transferType);
    

    /**
     * Add a data plane instance
     */
    ServiceResult<Void> addInstance(DataPlaneInstance instance);

}
