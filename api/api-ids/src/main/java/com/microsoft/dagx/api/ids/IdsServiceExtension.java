package com.microsoft.dagx.api.ids;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

public class IdsServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        WebService webService = context.getService(WebService.class);

        // webService.registerController();

        monitor.info("Initialized IDS API extension");
    }

    @Override
    public void start() {
        monitor.info("Started IDS API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown IDS API extension");
    }
}
