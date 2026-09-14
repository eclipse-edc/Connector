/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.defaults.storage.dataplane;

import org.eclipse.edc.connector.dataplane.selector.spi.testfixtures.store.DataPlaneInstanceStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

class InMemoryDataPlaneInstanceStoreTest extends DataPlaneInstanceStoreTestBase {

    private InMemoryDataPlaneInstanceStore store;

    @BeforeEach
    void setup() {
        store = new InMemoryDataPlaneInstanceStore(CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    public InMemoryDataPlaneInstanceStore getStore() {
        return store;
    }

}
