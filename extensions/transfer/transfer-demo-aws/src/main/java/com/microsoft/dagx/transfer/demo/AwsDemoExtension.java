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
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;

import java.util.Set;

public class AwsDemoExtension implements ServiceExtension {

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var dataFlowMgr = context.getService(DataFlowManager.class);

        var flowController = new DemoFlowController(context.getService(Vault.class), context.getService(ClientProvider.class));

        dataFlowMgr.register(flowController);
    }

    @Override
    public Set<String> requires() {
        return Set.of("client-provider");
    }
}
