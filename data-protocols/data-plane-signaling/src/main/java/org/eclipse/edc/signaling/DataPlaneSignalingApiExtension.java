/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.signaling.port.api.DataPlaneRegistrationApiController;
import org.eclipse.edc.signaling.port.api.DataPlaneTransferApiController;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import static org.eclipse.edc.signaling.DataPlaneSignalingApiExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingApiExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling Api";

    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessService transferProcessService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.CONTROL, new DataPlaneRegistrationApiController(dataPlaneSelectorService));
        webService.registerResource(ApiContext.CONTROL, new DataPlaneTransferApiController(transferProcessService));
    }


}
