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

package org.eclipse.dataspaceconnector.dataplane.selector.store;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Holds all {@link DataPlaneInstance} objects that are known to the DPF selector.
 * The collection of {@link DataPlaneInstance} objects is mutable at runtime, so implementations must take that into account.
 */
public interface DataPlaneInstanceStore {
    void save(DataPlaneInstance instance);

    void saveAll(Collection<DataPlaneInstance> instances);

    DataPlaneInstance findById(String id);

    Stream<DataPlaneInstance> getAll();

}
