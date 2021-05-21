/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */
package com.microsoft.dagx.transfer.demo;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.flow.DataFlowManager;

public class AwsDemoExtension implements ServiceExtension {

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var dataFlowMgr = context.getService(DataFlowManager.class);

        var flowController = new DemoS3FlowController(context.getService(Vault.class), monitor);

        dataFlowMgr.register(flowController);
    }
}
