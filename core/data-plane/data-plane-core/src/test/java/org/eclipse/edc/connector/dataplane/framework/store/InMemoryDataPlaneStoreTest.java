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

import org.eclipse.edc.connector.core.store.CriterionOperatorRegistryImpl;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;

import java.time.Clock;
import java.time.Duration;

class InMemoryDataPlaneStoreTest extends DataPlaneStoreTestBase {

    private final InMemoryDataPlaneStore store = new InMemoryDataPlaneStore(CONNECTOR_NAME, Clock.systemUTC(), CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected DataPlaneStore getStore() {
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
