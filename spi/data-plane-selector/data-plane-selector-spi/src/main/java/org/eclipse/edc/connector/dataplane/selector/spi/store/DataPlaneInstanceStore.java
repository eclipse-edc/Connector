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

package org.eclipse.edc.connector.dataplane.selector.spi.store;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.persistence.StateEntityStore;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

/**
 * Holds all {@link DataPlaneInstance} objects that are known to the DPF selector.
 * The collection of {@link DataPlaneInstance} objects is mutable at runtime, so implementations must take that into account.
 */
public interface DataPlaneInstanceStore extends StateEntityStore<DataPlaneInstance> {

    /**
     * Delete a data plane instance by its id.
     *
     * @param instanceId the data plane instance id.
     * @return the deleted data plane instance.
     */
    StoreResult<DataPlaneInstance> deleteById(String instanceId);

    Stream<DataPlaneInstance> getAll();

}
