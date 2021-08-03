/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.transfer.store.memory;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.transfer.store.TransferProcessStore;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link org.eclipse.edc.spi.transfer.store.TransferProcessStore} for testing.
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
        return Set.of("edc:transferprocessstore");
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

