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

import org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Duration;

class InMemoryDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {

    private InMemoryDataPlaneInstanceStore store;

    @BeforeEach
    void setup() {
        store = new InMemoryDataPlaneInstanceStore(CONNECTOR_NAME, Clock.systemUTC(), CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    public InMemoryDataPlaneInstanceStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String entityId, String owner, Duration duration) {
        store.acquireLease(entityId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String entityId, String owner) {
        return store.isLeasedBy(entityId, owner);
    }
}
