/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo.api;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;

public class ClientApiControllerExtension implements ServiceExtension {

    @EdcSetting
    private static final String DESTINATION_REGION = "edc.transfer.demo.s3.destination.region";

    @EdcSetting
    private static final String DESTINATION_BUCKET = "edc.transfer.demo.s3.destination.bucket";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var webService = context.getService(WebService.class);
        var dispatcherRegistry = context.getService(RemoteMessageDispatcherRegistry.class);
        var processManager = context.getService(TransferProcessManager.class);

        String destinationRegion = context.getSetting(DESTINATION_REGION, "test-region");
        String destinationBucket = context.getSetting(DESTINATION_BUCKET, "test-bucket");

        webService.registerController(new ClientApiController(dispatcherRegistry, processManager, monitor, destinationRegion, destinationBucket));

        monitor.info("Initialized Client API extension");
    }

    @Override
    public void start() {
        monitor.info("Started Client API extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Client API extension");
    }


}
