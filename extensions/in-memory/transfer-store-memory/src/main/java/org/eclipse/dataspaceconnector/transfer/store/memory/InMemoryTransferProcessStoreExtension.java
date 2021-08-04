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

package org.eclipse.dataspaceconnector.transfer.store.memory;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore} for testing.
 */
public class InMemoryTransferProcessStoreExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(TransferProcessStore.class, new InMemoryTransferProcessStore());
        monitor = context.getMonitor();
        monitor.info("Initialized In-Memory Transfer Process Store extension");
    }

    @Override
    public Set<String> provides() {
        return Set.of("dataspaceconnector:transferprocessstore");
    }

    @Override
    public void start() {
        monitor.info("Started Initialized In-Memory Transfer Process Store extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Initialized In-Memory Transfer Process Store extension");
    }

}

