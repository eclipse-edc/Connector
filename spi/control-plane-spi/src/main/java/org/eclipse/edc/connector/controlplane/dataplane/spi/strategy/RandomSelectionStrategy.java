/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.dataplane.spi.strategy;

import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Selects one {@link DataPlaneInstance} at random.
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
