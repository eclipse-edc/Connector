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
 *    Microsoft Corporation - name refactoring
 */

package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class AssetApiExtension implements ServiceExtension {

    @Inject
    private WebService webService;

    @Inject
    private DataManagementApiConfiguration config;

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        Monitor monitor = serviceExtensionContext.getMonitor();
        webService.registerResource(config.getContextAlias(), new AssetController(monitor));
    }
}
