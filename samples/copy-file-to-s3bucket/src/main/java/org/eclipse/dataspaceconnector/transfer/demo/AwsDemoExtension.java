/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */
package org.eclipse.dataspaceconnector.transfer.demo;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;

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
