package com.microsoft.dagx.metadata.memory;

import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

public class InMemoryServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        context.registerService(MetadataStore.class, new InMemoryMetadataStore());

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
