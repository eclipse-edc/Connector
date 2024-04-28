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

package org.eclipse.edc.connector.controlplane.defaults.storage.policydefinition;

import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

class InMemoryPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {
    private PolicyDefinitionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPolicyDefinitionStore(CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    protected PolicyDefinitionStore getPolicyDefinitionStore() {
        return store;
    }

}
