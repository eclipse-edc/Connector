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

package org.eclipse.edc.connector.dataplane.selector.store;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstanceStates;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Default (=in-memory) implementation for the {@link DataPlaneInstanceStore}.
 */
public class InMemoryDataPlaneInstanceStore extends InMemoryStatefulEntityStore<DataPlaneInstance> implements DataPlaneInstanceStore {

    public InMemoryDataPlaneInstanceStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryDataPlaneInstanceStore(String owner, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(DataPlaneInstance.class, owner, clock, criterionOperatorRegistry, state -> DataPlaneInstanceStates.valueOf(state).code());
    }

    @Override
    public StoreResult<DataPlaneInstance> deleteById(String instanceId) {
        var instance = findById(instanceId);
        if (instance == null) {
            return StoreResult.notFound("Data plane instance %s not found".formatted(instanceId));
        }
        delete(instanceId);
        return StoreResult.success(instance);
    }

    @Override
    public Stream<DataPlaneInstance> getAll() {
        return findAll();
    }
}
