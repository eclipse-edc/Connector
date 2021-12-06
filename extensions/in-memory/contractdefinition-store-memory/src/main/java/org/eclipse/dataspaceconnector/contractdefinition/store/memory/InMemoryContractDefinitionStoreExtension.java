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

import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link ContractDefinitionStore}.
 */
public class InMemoryContractDefinitionStoreExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(ContractDefinitionStore.class, new InMemoryContractDefinitionStore());
    }

    @Override
    public Set<String> provides() {
        return Set.of(ContractDefinitionStore.FEATURE);
    }
}
