/*
 * Copyright (c) 2022 Diego Gomez
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   Diego Gomez - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class AssetControllerServiceExtension implements ServiceExtension {

    private Monitor monitor;
    @Inject
    private WebService webService;

    @Override
    public String name() {
        return "EDC Control API";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        monitor = serviceExtensionContext.getMonitor();

        webService.registerController(new AssetController(monitor));
    }

}
