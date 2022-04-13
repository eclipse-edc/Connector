/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extensions.api;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusChecker;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ApiEndpointExtension implements ServiceExtension {
    @Inject
    WebService webService;

    @Inject
    TransferProcessManager processManager;

    @Inject
    ConsumerContractNegotiationManager negotiationManager;

    @Inject
    TransferProcessStore transferProcessStore;

    @Inject
    StatusCheckerRegistry statusCheckerRegistry;

    @Override
    public String name() {
        return "API Endpoint";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource("data", new ConsumerApiController(context.getMonitor(), processManager, negotiationManager, transferProcessStore));
        statusCheckerRegistry.register("File", (transferProcess, resources) -> {
            var file = new File(transferProcess.getDataRequest().getDataDestination().getProperty("path"));
            return file.exists() && file.isDirectory() && file.listFiles().length > 0;
        });
    }
}
