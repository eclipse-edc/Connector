/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.store.memory;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link com.microsoft.dagx.spi.transfer.store.TransferProcessStore} for testing.
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
        return Set.of("dagx:transferprocessstore");
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

