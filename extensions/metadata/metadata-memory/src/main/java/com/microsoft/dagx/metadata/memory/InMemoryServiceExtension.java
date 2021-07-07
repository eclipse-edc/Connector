/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.metadata.memory;

import com.microsoft.dagx.spi.metadata.MetadataObservable;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public Set<String> provides() {
        return Set.of("dagx:metadata-store-observable");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        final InMemoryMetadataStore service = new InMemoryMetadataStore();
        context.registerService(MetadataStore.class, service);
        context.registerService(MetadataObservable.class, service);

        monitor.info("Initialized In-Memory Metadata extension");
    }

    @Override
    public void start() {
        monitor.info("Started In-Memory Metadata extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown In-Memory Metadata extension");
    }
}
