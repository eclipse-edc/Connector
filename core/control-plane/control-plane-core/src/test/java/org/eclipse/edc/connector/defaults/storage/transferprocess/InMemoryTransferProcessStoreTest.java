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
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

import java.time.Duration;

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

}
