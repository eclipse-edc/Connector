/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.negotiation.store.memory;

import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link ContractNegotiationStore} for testing.
 */
public class InMemoryContractNegotiationStoreExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Contract Negotiation Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(ContractNegotiationStore.class, new InMemoryContractNegotiationStore());
    }

    @Override
    public Set<String> provides() {
        return Set.of(ContractNegotiationStore.FEATURE);
    }

}
