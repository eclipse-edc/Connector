/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.control.http;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

/**
 * Provides a control plane over HTTP.
 */
public class HttpControlServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        WebService webService = context.getService(WebService.class);

        webService.registerController(new PingController());

        monitor.info("Initialized HTTP Control extension");
    }

    @Override
    public void start() {
        monitor.info("Started HTTP Control extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown HTTP Control extension");
    }

}
