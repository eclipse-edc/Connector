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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects one {@link org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance} at random.
 */
public class RandomSelectionStrategy implements SelectionStrategy {
    private final ThreadLocalRandom random;

    public RandomSelectionStrategy() {
        random = ThreadLocalRandom.current();
    }

    /**
     * Guaranteed to always produce a non-null result given that the instances list is not empty
     */
    @Override
    public DataPlaneInstance apply(List<DataPlaneInstance> instances) {
        if (instances.isEmpty()) {
            return null;
        }
        var index = random.nextInt(instances.size());
        return instances.get(index);
    }

    @Override
    public String getName() {
        return "random";
    }
}
