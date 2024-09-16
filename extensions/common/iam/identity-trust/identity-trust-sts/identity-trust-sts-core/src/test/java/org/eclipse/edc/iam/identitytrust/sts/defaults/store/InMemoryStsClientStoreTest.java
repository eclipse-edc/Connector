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

package org.eclipse.edc.iam.identitytrust.sts.defaults.store;

import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.StsClientStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

public class InMemoryStsClientStoreTest extends StsClientStoreTestBase {

    private InMemoryStsClientStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryStsClientStore(CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    protected StsClientStore getStsClientStore() {
        return store;
    }
}
