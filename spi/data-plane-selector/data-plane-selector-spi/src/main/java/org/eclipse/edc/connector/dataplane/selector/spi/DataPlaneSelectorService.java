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

import java.util.List;

/**
 * Main interaction interface for an EDC runtime (=control plane) to communicate with the DPF selector.
 */
@ExtensionPoint
public interface DataPlaneSelectorService {

    /**
     * Returns all {@link DataPlaneInstance}s known in the system
     */
    List<DataPlaneInstance> getAll();

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the configured
     * strategy.
     */
    DataPlaneInstance select(DataAddress source, DataAddress destination);

    /**
     * Selects the {@link DataPlaneInstance} that can handle a source and destination {@link DataAddress} using the passed
     * strategy.
     */
    DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy);

    /**
     * Add a data plane instance
     */
    ServiceResult<Void> addInstance(DataPlaneInstance instance);

}
