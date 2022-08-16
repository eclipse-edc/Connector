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

package org.eclipse.dataspaceconnector.dataplane.selector.strategy;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;

import java.util.List;
import java.util.function.Function;

/**
 * Interface for different algorithms by which the {@link org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelector}
 * selects a particular {@link org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance}
 */
public interface SelectionStrategy extends Function<List<DataPlaneInstance>, DataPlaneInstance> {

    /**
     * Applies its selection algorithm to a collection of {@link DataPlaneInstance} objects.
     *
     * @param instances The list of all {@link DataPlaneInstance} objects
     * @return If the list of instances is non-empty, every selection strategy must always return a result.
     */
    @Override
    DataPlaneInstance apply(List<DataPlaneInstance> instances);

    default String getName() {
        return getClass().getCanonicalName();
    }
}
