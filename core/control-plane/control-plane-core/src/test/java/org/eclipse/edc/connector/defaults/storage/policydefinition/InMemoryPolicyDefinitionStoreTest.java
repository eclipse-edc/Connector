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

package org.eclipse.edc.connector.defaults.storage.policydefinition;

import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.policy.spi.testfixtures.store.PolicyDefinitionStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.util.concurrency.LockManager;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.locks.ReentrantReadWriteLock;

class InMemoryPolicyDefinitionStoreTest extends PolicyDefinitionStoreTestBase {
    private PolicyDefinitionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryPolicyDefinitionStore(new LockManager(new ReentrantReadWriteLock(true)), CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    protected PolicyDefinitionStore getPolicyDefinitionStore() {
        return store;
    }

}
