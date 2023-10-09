/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.monitor.store.sql;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.policy.monitor.spi.testfixtures.store.PolicyMonitorStoreTestBase;

import java.time.Clock;
import java.time.Duration;

class InMemoryPolicyMonitorStoreTest extends PolicyMonitorStoreTestBase {

    private final InMemoryPolicyMonitorStore store = new InMemoryPolicyMonitorStore(CONNECTOR_NAME, Clock.systemUTC());

    @Override
    protected PolicyMonitorStore getStore() {
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
