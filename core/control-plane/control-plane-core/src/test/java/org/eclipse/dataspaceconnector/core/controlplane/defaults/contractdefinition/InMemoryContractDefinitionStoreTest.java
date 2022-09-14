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

package org.eclipse.dataspaceconnector.core.controlplane.defaults.contractdefinition;

import org.eclipse.dataspaceconnector.contract.offer.store.ContractDefinitionStoreTestBase;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;

class InMemoryContractDefinitionStoreTest extends ContractDefinitionStoreTestBase {
    private final InMemoryContractDefinitionStore store = new InMemoryContractDefinitionStore();


    @Override
    protected ContractDefinitionStore getContractDefinitionStore() {
        return store;
    }


    @Override
    protected Boolean supportCollectionQuery() {
        return false;
    }

    @Override
    protected Boolean supportSortOrder() {
        return true;
    }
}
