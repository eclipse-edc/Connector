package com.microsoft.dagx.control.http;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import static com.microsoft.dagx.spi.system.ServiceExtension.LoadPhase.DEFAULT;

/**
 * Provides a control plane over HTTP.
 */
public class HttpControlServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public LoadPhase phase() {
        return DEFAULT;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        WebService webService = context.getService(WebService.class);

        webService.registerController(new PingController());

        monitor.info("Initialized HTTP control extension");
    }

    @Override
    public void start() {
        monitor.info("Started HTTP control extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown HTTP control extension");
    }

}
