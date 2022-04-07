/*
 *  Copyright (c) 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.samples.identity.registrationservice.api;

import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.iam.RegistrationService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class RegistrationServiceApiExtension implements ServiceExtension {

    @Inject
    private DidStore didDocumentStore;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // create the registration service, which offers a REST API
        var regSrv = new RegistrationServiceController(context.getMonitor(), didDocumentStore);
        context.registerService(RegistrationService.class, regSrv);

        // register the service as REST controller
        var webService = context.getService(WebService.class);
        webService.registerResource(regSrv);

        context.getMonitor().info("Registration Service REST API initialized");
    }
}
