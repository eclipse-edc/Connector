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

import org.eclipse.edc.connector.core.store.InMemoryStatefulEntityStore;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;

import java.time.Clock;
import java.util.UUID;

/**
 * Implements an in-memory, ephemeral store with a maximum capacity. If the store grows beyond capacity, the oldest entry will be evicted.
 */
public class InMemoryDataPlaneStore extends InMemoryStatefulEntityStore<DataFlow> implements DataPlaneStore {

    public InMemoryDataPlaneStore(Clock clock) {
        this(UUID.randomUUID().toString(), clock);
    }

    public InMemoryDataPlaneStore(String connectorName, Clock clock) {
        super(DataFlow.class, connectorName, clock);
    }
}
