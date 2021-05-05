/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.demo.ui;

import com.microsoft.dagx.spi.message.RemoteMessageDispatcherRegistry;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.protocol.web.WebService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.TransferProcessManager;

/**
 *
 */
public class DemoUiApiExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var webService = context.getService(WebService.class);
        var dispatcherRegistry = context.getService(RemoteMessageDispatcherRegistry.class);
        var processManager = context.getService(TransferProcessManager.class);

        webService.registerController(new DemoUiApiController(dispatcherRegistry, processManager, monitor));

        monitor.info("Initialized Demo UI API extension");
    }

    @Override
    public void start() {
        monitor.info("Started Demo UI API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Demo UI Catalog API extension");
    }


}
