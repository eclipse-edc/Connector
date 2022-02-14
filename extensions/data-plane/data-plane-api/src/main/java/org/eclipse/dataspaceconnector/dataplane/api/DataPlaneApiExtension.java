/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.dataplane.api;

import org.eclipse.dataspaceconnector.dataplane.api.transfer.DataPlaneTransferController;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Provides the control plane and public APIs for a data plane server.
 */
public class DataPlaneApiExtension implements ServiceExtension {
    private static final String CONTROL = "control";

    @Inject
    private DataPlaneManager dataPlaneManager;

    @Inject
    private WebService webService;

    @Override
    public String name() {
        return "Data Plane API";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(CONTROL, new DataPlaneTransferController(dataPlaneManager));
    }
}


