/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class AssetControllerServiceExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        Monitor monitor = serviceExtensionContext.getMonitor();
        //avoid jetty throwing exceptions down the road
        if (!serviceExtensionContext.getConfig().hasKey("web.http.data.port")) {
            monitor.severe("No port mapping entry for context 'data' ('web.http.data.port=...') found in configuration. The Data Management API will not be available!");
        } else {
            // todo: also register the Authorization filter, once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/pull/598 is finished:
            // webService.registerResource("data", new AuthorizationRequestFilter());
            webService.registerResource("data", new AssetController(monitor));
        }
    }
}
