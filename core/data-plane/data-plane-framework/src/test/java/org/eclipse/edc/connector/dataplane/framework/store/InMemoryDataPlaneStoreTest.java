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

import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.spi.testfixtures.store.DataPlaneStoreTestBase;
import org.junit.jupiter.api.BeforeEach;

class InMemoryDataPlaneStoreTest extends DataPlaneStoreTestBase {
    private InMemoryDataPlaneStore store;


    @BeforeEach
    void setUp() {
        store = new InMemoryDataPlaneStore(2);
    }

    @Override
    protected DataPlaneStore getStore() {
        return store;
    }
}
