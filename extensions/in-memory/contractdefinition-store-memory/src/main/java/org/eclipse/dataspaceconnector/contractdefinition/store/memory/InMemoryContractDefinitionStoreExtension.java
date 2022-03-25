/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.contractdefinition.store.memory;

import org.eclipse.dataspaceconnector.dataloading.ContractDefinitionLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides an in-memory implementation of the {@link ContractDefinitionStore}.
 */
@Provides({ ContractDefinitionStore.class, ContractDefinitionLoader.class })
public class InMemoryContractDefinitionStoreExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Contract Definition Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new InMemoryContractDefinitionStore();
        context.registerService(ContractDefinitionStore.class, store);
        context.registerService(ContractDefinitionLoader.class, store::save);
    }
}
