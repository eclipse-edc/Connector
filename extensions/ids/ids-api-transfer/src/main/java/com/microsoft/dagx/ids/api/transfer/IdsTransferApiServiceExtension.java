package com.microsoft.dagx.ids.api.transfer;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

/**
 * Implements the IDS Controller REST API for data transfer services.
 */
public class IdsTransferApiServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        registerTypes(context);

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
        WebService webService = context.getService(WebService.class);
        webService.registerController(new ArtifactRequestController(context.getMonitor()));
    }

    private void registerTypes(ServiceExtensionContext context) {

    }


}
