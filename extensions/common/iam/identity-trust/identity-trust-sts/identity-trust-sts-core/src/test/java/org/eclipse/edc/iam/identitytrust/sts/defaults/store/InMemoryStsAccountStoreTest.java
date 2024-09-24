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

import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsAccountStore;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.StsAccountStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.junit.jupiter.api.BeforeEach;

public class InMemoryStsAccountStoreTest extends StsAccountStoreTestBase {

    private InMemoryStsAccountStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryStsAccountStore(CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Override
    protected StsAccountStore getStsClientStore() {
        return store;
    }
}
