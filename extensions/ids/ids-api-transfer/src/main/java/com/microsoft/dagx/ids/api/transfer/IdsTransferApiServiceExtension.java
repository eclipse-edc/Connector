package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.ids.spi.daps.DapsService;
import com.microsoft.dagx.spi.metadata.MetadataStore;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;

import java.util.Set;

/**
 * Implements the IDS Controller REST API for data transfer services.
 */
public class IdsTransferApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> requires() {
        return Set.of("ids.core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerControllers(context);

        monitor.info("Initialized IDS Transfer API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS Transfer API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS Transfer API extension");
    }

    private void registerControllers(ServiceExtensionContext context) {

        var webService = context.getService(WebService.class);

        var dapService = context.getService(DapsService.class);

        var transferManagerRegistry = context.getService(DataFlowManager.class);

        var metadataStore = context.getService(MetadataStore.class);

        var monitor = context.getMonitor();

        webService.registerController(new ArtifactRequestController(dapService, metadataStore, transferManagerRegistry, monitor));
    }


}
