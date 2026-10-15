/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.controlplane.defaults.storage.transferprocess;

import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TransferProcessStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.testfixtures.store.TestFunctions.createTransferProcess;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

class InMemoryTransferProcessStoreTest extends TransferProcessStoreTestBase {

    private final InMemoryTransferProcessStore store = new InMemoryTransferProcessStore(CONNECTOR_NAME, clock, CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected TransferProcessStore getTransferProcessStore() {
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

    @Test
    void nextNotLeased_shouldReturnEntityAgain_whenConfiguredLeaseDurationExpired() {
        var shortLeaseStore = new InMemoryTransferProcessStore(clock, CriterionOperatorRegistryImpl.ofDefaults(), Duration.ofMillis(500));
        shortLeaseStore.save(createTransferProcess("id1", INITIAL));

        assertThat(shortLeaseStore.nextNotLeased(1, hasState(INITIAL.code()))).hasSize(1);
        assertThat(shortLeaseStore.nextNotLeased(1, hasState(INITIAL.code()))).isEmpty();
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(shortLeaseStore.nextNotLeased(1, hasState(INITIAL.code()))).hasSize(1));
    }
}
