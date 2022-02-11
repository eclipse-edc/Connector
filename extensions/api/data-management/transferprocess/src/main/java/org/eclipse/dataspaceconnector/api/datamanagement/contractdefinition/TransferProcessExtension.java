/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class TransferProcessExtension implements ServiceExtension {
    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        //avoid jetty throwing exceptions down the road
        if (!context.getConfig().hasKey("web.http.data.port")) {
            context.getMonitor().severe("No port mapping entry for context 'data' ('web.http.data.port=...') found in configuration. The Data Management API will not be available!");
        } else {
            // todo: also register the Authorization filter, once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/pull/598 is finished:
            // webService.registerResource("data", new AuthorizationRequestFilter());
            webService.registerResource("data", new TransferProcessApiController(context.getMonitor()));
        }
    }
}