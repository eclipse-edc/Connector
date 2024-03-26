/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.framework.store;

import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataStore;
import org.eclipse.edc.connector.dataplane.spi.store.AccessTokenDataTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryAccessTokenDataStoreTest extends AccessTokenDataTestBase {
    private final InMemoryAccessTokenDataStore store = new InMemoryAccessTokenDataStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected AccessTokenDataStore getStore() {
        return store;
    }
}