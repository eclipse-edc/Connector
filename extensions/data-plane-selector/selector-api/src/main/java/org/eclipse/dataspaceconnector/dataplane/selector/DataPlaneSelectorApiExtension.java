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

package org.eclipse.dataspaceconnector.dataplane.selector;

import org.eclipse.dataspaceconnector.dataplane.selector.api.DataplaneSelectorApiController;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstanceImpl;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class DataPlaneSelectorApiExtension implements ServiceExtension {

    public static final String DATAPLANE_SELECTOR_CONTEXTALIAS = "dataplane";
    @Inject
    private WebService webservice;
    @Inject
    private DataPlaneSelectorService selectionService;

    @Override
    public void initialize(ServiceExtensionContext context) {

        //todo: add authentication
        //var filter = new AuthenticationRequestFilter();

        context.getTypeManager().registerTypes(DataPlaneInstanceImpl.class);

        var controller = new DataplaneSelectorApiController(selectionService);
        webservice.registerResource(DATAPLANE_SELECTOR_CONTEXTALIAS, controller);
    }
}
