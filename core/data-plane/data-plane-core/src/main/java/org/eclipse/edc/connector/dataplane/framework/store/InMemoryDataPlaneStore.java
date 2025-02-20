/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.framework.store;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.DataFlowStates;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.store.InMemoryStatefulEntityStore;

import java.time.Clock;
import java.util.UUID;

/**
 * Implements an in-memory, ephemeral store with a maximum capacity. If the store grows beyond capacity, the oldest entry will be evicted.
 */
public class InMemoryDataPlaneStore extends InMemoryStatefulEntityStore<DataFlow> implements DataPlaneStore {

    public InMemoryDataPlaneStore(Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        this(UUID.randomUUID().toString(), clock, criterionOperatorRegistry);
    }

    public InMemoryDataPlaneStore(String connectorName, Clock clock, CriterionOperatorRegistry criterionOperatorRegistry) {
        super(DataFlow.class, connectorName, clock, criterionOperatorRegistry, state -> DataFlowStates.valueOf(state).code());
    }
}
