/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.defaults.storage.dataspaceprofile;

import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

class InMemoryDataspaceProfileStoreTest extends DataspaceProfileStoreTestBase {

    private DataspaceProfileStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataspaceProfileStore(CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    protected DataspaceProfileStore getStore() {
        return store;
    }
}
