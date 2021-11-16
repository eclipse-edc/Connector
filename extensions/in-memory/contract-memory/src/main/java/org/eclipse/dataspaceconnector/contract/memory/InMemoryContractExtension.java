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

package org.eclipse.dataspaceconnector.contract.memory;

import org.eclipse.dataspaceconnector.spi.contract.ContractStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryContractExtension implements ServiceExtension {
    private static final String NAME = "In-Memory Contract Store Extension";

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of(ContractStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        ContractStore contractStore = new InMemoryContractStore();
        context.registerService(ContractStore.class, contractStore);

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {

        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }
}
