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

package org.eclipse.edc.connector.defaults.storage.transferprocess;

import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.testfixtures.store.TransferProcessStoreTestBase;
import org.eclipse.edc.spi.persistence.Lease;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class InMemoryTransferProcessStoreTest extends TransferProcessStoreTestBase {
    private final Map<String, Lease> leases = new HashMap<>();
    private InMemoryTransferProcessStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTransferProcessStore(CONNECTOR_NAME, Clock.systemUTC(), leases);
    }


    @Override
    protected boolean supportsCollectionQuery() {
        return false;
    }

    @Override
    protected boolean supportsLikeOperator() {
        return false;
    }

    @Override
    protected TransferProcessStore getTransferProcessStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String negotiationId, String owner, Duration duration) {
        leases.put(negotiationId, new Lease(owner, Clock.systemUTC().millis(), duration.toMillis()));
    }

    @Override
    protected boolean isLeasedBy(String negotiationId, String owner) {
        return leases.entrySet().stream().anyMatch(e -> e.getKey().equals(negotiationId) &&
                e.getValue().getLeasedBy().equals(owner) &&
                !isExpired(e.getValue()));
    }

    private boolean isExpired(Lease e) {
        return e.getLeasedAt() + e.getLeaseDuration() < Clock.systemUTC().millis();
    }


}
